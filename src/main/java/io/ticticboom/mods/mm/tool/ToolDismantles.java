package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.DismantleJob;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.builder.ItemSink;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dismantling a machine with the multiblock tool: what it breaks, the Shift+right-click confirmation, starting it. */
public final class ToolDismantles {
    /** How long the first Shift+right-click waits for the second: 3 s. */
    public static final int CONFIRM_TICKS = 60;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ToolDismantles() {
    }

    private record Pending(ResourceKey<Level> dimension, BlockPos controllerPos, long gameTime) {
    }

    /** A dismantle ready to run; blocks go into {@code sink}. */
    public record Prepared(BlockPos controllerPos, List<DismantlePlanner.Target> positions, ItemSink sink, int perBlockFe, ItemStack tool) {
        public DismantleJob job(ServerLevel level) {
            return DismantleJob.create(level, controllerPos, positions, perBlockFe, tool);
        }
    }

    /** Exactly one of {@code prepared} and {@code error} is set. */
    public record Result(@Nullable Prepared prepared, @Nullable Component error) {
        static Result error(Component error) {
            return new Result(null, error);
        }
    }

    /** What dismantling the machine aimed at (any of its blocks, or an unformed controller itself) would do. */
    public static Result prepare(Level level, Player player, ItemStack tool, BlockPos target) {
        MachineControllerBlockEntity controller = DismantlePlanner.resolve(level, target);
        BlockPos center;
        List<DismantlePlanner.Target> positions;
        if (controller != null) {
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(player, link.owner())) {
                return Result.error(Component.translatable("message.mm.tool.no_access"));
            }
            center = controller.getBlockPos();
            positions = DismantlePlanner.positions(level, controller);
        } else {
            DismantlePlanner.BuilderMatch builder = DismantlePlanner.matchBuilder(level, target, tool);
            if (builder == null) {
                return Result.error(Component.translatable("message.mm.tool.dismantle.no_machine"));
            }
            center = builder.center();
            positions = builder.positions();
        }
        if (!player.mayBuild() || !level.mayInteract(player, center)) {
            return Result.error(Component.translatable("message.mm.tool.dismantle.protected"));
        }
        int perBlockFe = MMConfigSetup.COMMON.toolEnergyPerDismantledBlock.get();
        ItemSink sink = ToolBuildPlan.sink(player, tool);
        if (!sink.free() && perBlockFe > 0
                && new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored() < perBlockFe) {
            return Result.error(Component.translatable("message.mm.assemble.out_of_energy", 0, positions.size()));
        }
        return new Result(new Prepared(center, positions, sink, perBlockFe, tool), null);
    }

    /**
     * Records a Shift+right-click on a machine. True when it confirms an earlier click on the same machine within
     * {@link #CONFIRM_TICKS}; the confirmation is then used up.
     */
    public static boolean confirm(Player player, ResourceKey<Level> dimension, BlockPos controllerPos, long gameTime) {
        Pending last = PENDING.get(player.getUUID());
        if (last != null && last.dimension().equals(dimension) && last.controllerPos().equals(controllerPos)
                && gameTime - last.gameTime() <= CONFIRM_TICKS) {
            PENDING.remove(player.getUUID());
            return true;
        }
        PENDING.put(player.getUUID(), new Pending(dimension, controllerPos.immutable(), gameTime));
        return false;
    }

    /** Shift+right-click on a machine: the first click asks, the second one within 3 s starts. */
    public static void shiftClick(ServerPlayer player, ItemStack tool, BlockPos target) {
        if (refuseBusy(player)) {
            return;
        }
        Result result = prepare(player.level(), player, tool, target);
        if (result.prepared() == null) {
            player.displayClientMessage(result.error(), true);
            return;
        }
        Prepared prepared = result.prepared();
        if (!confirm(player, player.level().dimension(), prepared.controllerPos(), player.level().getGameTime())) {
            player.displayClientMessage(Component.translatable("message.mm.tool.dismantle.confirm", prepared.positions().size()), true);
            return;
        }
        start(player, prepared);
    }

    /** Validated V-hold request: starts at once. */
    public static void start(ServerPlayer player, ItemStack tool, BlockPos target) {
        if (refuseBusy(player)) {
            return;
        }
        Result result = prepare(player.level(), player, tool, target);
        if (result.prepared() == null) {
            player.displayClientMessage(result.error(), true);
            return;
        }
        start(player, result.prepared());
    }

    private static void start(ServerPlayer player, Prepared prepared) {
        if (!AssemblyJobs.startDismantle(player, prepared.job(player.serverLevel()), prepared.sink())) {
            refuseBusy(player);
        }
    }

    /** Checked before any planning: a player runs one job at a time. */
    private static boolean refuseBusy(ServerPlayer player) {
        Component busy = AssemblyJobs.busyMessage(player);
        if (busy != null) {
            player.displayClientMessage(busy, true);
        }
        return busy != null;
    }

    /** Forgets pending confirmations (server stopping). */
    public static void clear() {
        PENDING.clear();
    }
}
