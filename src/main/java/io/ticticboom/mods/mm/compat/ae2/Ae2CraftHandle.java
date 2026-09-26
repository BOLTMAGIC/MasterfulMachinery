package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import io.ticticboom.mods.mm.builder.me.CraftHandle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * One craft the Multiblock Tool asked AE2 for: the crafting plan is calculated off-thread, then submitted as a
 * standalone job (as a terminal does), whose output goes into the network's storage.
 * <p>
 * AE2 15 gives no link for a job submitted without a requester, and never marks a standalone job's link done, so the
 * job is followed through its crafting CPU: the CPU's link to it is cancelled, or the CPU has moved on (done).
 */
final class Ae2CraftHandle implements CraftHandle {
    private final Item item;
    private final int amount;
    private final IGrid grid;
    private final IActionSource source;
    private final Future<ICraftingPlan> calculation;
    private State state = State.CALCULATING;
    @Nullable
    private Component failure;
    @Nullable
    private CraftingCPUCluster cpu;
    @Nullable
    private ICraftingLink link;

    private Ae2CraftHandle(Item item, int amount, IGrid grid, IActionSource source, Future<ICraftingPlan> calculation) {
        this.item = item;
        this.amount = amount;
        this.grid = grid;
        this.source = source;
        this.calculation = calculation;
    }

    /** Starts calculating a plan for amount of item, as player. */
    static CraftHandle start(ServerPlayer player, IGrid grid, Item item, int amount) {
        IActionSource source = IActionSource.ofPlayer(player);
        // the calculation only looks for patterns on the requester's grid: without a node it finds none
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
        return new Ae2CraftHandle(item, amount, grid, source, calculation);
    }

    @Override
    public void update() {
        if (state == State.CALCULATING && calculation.isDone()) {
            submit();
        } else if (state == State.CRAFTING) {
            follow();
        }
    }

    /** The plan is ready (never waited for): submit it, or fail when it lacks ingredients. */
    private void submit() {
        ICraftingPlan plan;
        try {
            plan = calculation.get();
        } catch (Exception e) {
            fail("message.mm.tool.craft.error");
            return;
        }
        if (plan == null || plan.simulation()) {
            fail("message.mm.tool.craft.missing_ingredients");
            return;
        }
        ICraftingService crafting = grid.getCraftingService();
        Map<ICraftingCPU, ICraftingLink> before = new IdentityHashMap<>();
        for (ICraftingCPU each : crafting.getCpus()) {
            if (each instanceof CraftingCPUCluster cluster) {
                before.put(cluster, cluster.craftingLogic.getLastLink());
            }
        }
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
        // the CPU that took the job is the one whose job changed
        for (ICraftingCPU each : crafting.getCpus()) {
            if (each instanceof CraftingCPUCluster cluster) {
                ICraftingLink now = cluster.craftingLogic.getLastLink();
                if (now != null && now != before.get(cluster)) {
                    cpu = cluster;
                    link = now;
                }
            }
        }
        state = State.CRAFTING;
    }

    private void follow() {
        if (link == null || cpu == null) {
            // submitted, but its CPU could not be told apart: AE2 crafts on, the next right-click will see the items
            state = State.DONE;
        } else if (link.isCanceled() || cpu.isDestroyed()) {
            fail("message.mm.tool.craft.cancelled");
        } else if (link.isDone() || cpu.craftingLogic.getLastLink() != link) {
            state = State.DONE;
        }
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
