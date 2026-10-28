package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.me.CraftHandle;
import io.ticticboom.mods.mm.builder.me.CraftProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class Ae2CraftHandle implements CraftHandle {
    private static final int SHORT_POLLS = 2;
    private static final int CPU_GONE_POLLS = 10;

    private final Item item;
    private final int amount;
    private final AEItemKey key;
    private IGrid grid;
    private final Supplier<IGrid> network;
    private final IActionSource source;
    private final Future<ICraftingPlan> calculation;
    private State state = State.CALCULATING;
    @Nullable
    private Component failure;
    @Nullable
    private ICraftingCPU cpu;
    private long target;
    private int shortPolls;
    private boolean waiting;

    private Ae2CraftHandle(Item item, int amount, IGrid grid, Supplier<IGrid> network, IActionSource source, Future<ICraftingPlan> calculation) {
        this.item = item;
        this.amount = amount;
        this.key = AEItemKey.of(item);
        this.grid = grid;
        this.network = network;
        this.source = source;
        this.calculation = calculation;
    }

    static CraftHandle start(ServerPlayer player, IGrid grid, Supplier<IGrid> network, Item item, int amount) {
        IActionSource source = IActionSource.ofPlayer(player);
        IGridNode node = grid.getPivot();
        ICraftingSimulationRequester requester = new ICraftingSimulationRequester() {
            @Override
            public IActionSource getActionSource() {
                return source;
            }

            @Override
            public IGridNode getGridNode() {
                return node;
            }
        };
        Future<ICraftingPlan> calculation = grid.getCraftingService().beginCraftingCalculation(player.level(), requester,
                AEItemKey.of(item), amount, CalculationStrategy.REPORT_MISSING_ITEMS);
        return new Ae2CraftHandle(item, amount, grid, network, source, calculation);
    }

    @Override
    public void update() {
        if (!state.active()) {
            waiting = false;
            return;
        }
        IGrid now = network.get();
        waiting = now == null || !now.getEnergyService().isNetworkPowered();
        if (waiting) {
            return;
        }
        grid = now;
        if (state == State.CALCULATING && calculation.isDone()) {
            submit();
        } else if (state == State.CRAFTING) {
            follow();
        }
    }

    @Override
    public boolean waiting() {
        return waiting;
    }

    private void submit() {
        ICraftingPlan plan;
        try {
            plan = calculation.get();
        } catch (Exception e) {
            Ref.LOG.warn("ME crafting calculation for {} x{} failed", key, amount, e);
            fail("message.mm.tool.craft.error");
            return;
        }
        if (plan == null || plan.simulation()) {
            fail("message.mm.tool.craft.missing_ingredients");
            return;
        }
        ICraftingService crafting = grid.getCraftingService();
        List<ICraftingCPU> idle = crafting.getCpus().stream().filter(c -> !c.isBusy()).collect(Collectors.toList());
        long before = stock();
        ICraftingSubmitResult result = crafting.submitJob(plan, null, null, false, source);
        if (!result.successful()) {
            CraftingSubmitErrorCode code = result.errorCode();
            if (code == CraftingSubmitErrorCode.INCOMPLETE_PLAN || code == CraftingSubmitErrorCode.MISSING_INGREDIENT) {
                fail("message.mm.tool.craft.missing_ingredients");
            } else {
                fail(code == null ? "message.mm.tool.craft.error" : "message.mm.tool.craft.no_cpu");
            }
            return;
        }
        target = before + amount;
        cpu = ourCpu(idle);
        state = State.CRAFTING;
    }

    private @Nullable ICraftingCPU ourCpu(List<ICraftingCPU> idleBefore) {
        List<ICraftingCPU> started = new ArrayList<>();
        for (ICraftingCPU each : idleBefore) {
            if (each.isBusy()) {
                started.add(each);
            }
        }
        List<ICraftingCPU> confirmed = started.stream().filter(this::crafts).toList();
        if (confirmed.size() == 1) {
            return confirmed.get(0);
        }
        return started.size() == 1 && jobUnknown(started.get(0)) ? started.get(0) : null;
    }

    private boolean crafts(ICraftingCPU each) {
        CraftingJobStatus status = each.getJobStatus();
        return status != null && status.crafting() != null && key.equals(status.crafting().what());
    }

    private static boolean jobUnknown(ICraftingCPU each) {
        CraftingJobStatus status = each.getJobStatus();
        return status == null || status.crafting() == null;
    }

    private void follow() {
        ICraftingService crafting = grid.getCraftingService();
        Set<ICraftingCPU> cpus = crafting.getCpus();
        boolean cpuGone = cpu != null && !cpus.contains(cpu);
        boolean ourCpuCrafting = cpu != null && !cpuGone && cpu.isBusy() && crafts(cpu);
        boolean anyCpuCrafting = cpus.stream().anyMatch(each -> each.isBusy() && crafts(each));
        switch (CraftProgress.judge(stock(), target, ourCpuCrafting, anyCpuCrafting, crafting.isRequesting(key))) {
            case DONE -> state = State.DONE;
            case WAIT -> shortPolls = 0;
            case SHORT -> {
                if (++shortPolls >= (cpuGone ? CPU_GONE_POLLS : SHORT_POLLS)) {
                    fail("message.mm.tool.craft.ended");
                }
            }
        }
    }

    private long stock() {
        return grid.getStorageService().getInventory().extract(key, Long.MAX_VALUE, Actionable.SIMULATE, source);
    }

    private void fail(String reason) {
        state = State.FAILED;
        failure = Component.translatable(reason);
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public Item item() {
        return item;
    }

    @Override
    public int amount() {
        return amount;
    }

    @Override
    public @Nullable Component failure() {
        return failure;
    }
}
