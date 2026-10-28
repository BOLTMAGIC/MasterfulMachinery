package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AssemblyJob {
    final ServerLevel level;
    final BlockPos controllerPos;
    private final Deque<AssemblyPlanner.Planned> queue;
    private final int total;
    private final int perBlockFe;
    int placed;
    int blocked;
    final int unavailable;
    final Map<Block, Integer> missing = new LinkedHashMap<>();
    private boolean outOfEnergy;
    private boolean sourceGone;
    private boolean controllerNotPlaced;
    private final boolean requiresController;
    private final boolean layerByLayer;

    AssemblyJob(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe) {
        this(level, controllerPos, plan, perBlockFe, true);
    }

    AssemblyJob(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe, boolean requiresController) {
        this(level, controllerPos, plan, perBlockFe, requiresController, false);
    }

    AssemblyJob(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe, boolean requiresController, boolean layerByLayer) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.queue = new ArrayDeque<>(plan.steps());
        this.total = plan.steps().size();
        this.unavailable = plan.unavailable();
        this.perBlockFe = perBlockFe;
        this.requiresController = requiresController;
        this.layerByLayer = layerByLayer;
    }

    public static AssemblyJob create(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan) {
        return new AssemblyJob(level, controllerPos, plan, 0);
    }

    public static AssemblyJob create(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe) {
        return new AssemblyJob(level, controllerPos, plan, perBlockFe);
    }

    public static AssemblyJob createWithoutController(ServerLevel level, BlockPos center, AssemblyPlanner.Plan plan, int perBlockFe) {
        return new AssemblyJob(level, center, plan, perBlockFe, false);
    }

    public static AssemblyJob createTool(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe,
                                         boolean requiresController, io.ticticboom.mods.mm.tool.ToolBuildMode mode) {
        return new AssemblyJob(level, controllerPos, mode.order(plan, controllerPos, requiresController), perBlockFe,
                requiresController, mode == io.ticticboom.mods.mm.tool.ToolBuildMode.LAYER_BY_LAYER);
    }

    public int placed() {
        return placed;
    }

    public int blocked() {
        return blocked;
    }

    public int unavailable() {
        return unavailable;
    }

    public int total() {
        return total;
    }

    public boolean outOfEnergy() {
        return outOfEnergy;
    }

    public boolean sourceGone() {
        return sourceGone;
    }

    public boolean controllerNotPlaced() {
        return controllerNotPlaced;
    }

    public Map<Block, Integer> missing() {
        return Collections.unmodifiableMap(missing);
    }

    public boolean controllerPresent() {
        if (!requiresController) {
            return true;
        }
        AssemblyPlanner.Planned next = queue.peek();
        if (next != null && next.pos().equals(controllerPos)) {
            return true;
        }
        return level.isLoaded(controllerPos) && level.getBlockEntity(controllerPos) instanceof MachineControllerBlockEntity;
    }

    private enum Outcome { ALREADY, BLOCKED, MISSING, NO_ENERGY, FAILED, PLACED }

    public boolean tick(Player player, MaterialSource source, int budget) {
        Integer layer = null;
        while (budget > 0 && !queue.isEmpty()) {
            AssemblyPlanner.Planned peek = queue.peek();
            boolean controllerStep = requiresController && peek.pos().equals(controllerPos);
            if (layerByLayer && !controllerStep) {
                if (layer != null && peek.pos().getY() != layer) break;
                layer = peek.pos().getY();
            }
            if (source.gone()) {
                sourceGone = true;
                queue.clear();
                break;
            }
            AssemblyPlanner.Planned next = queue.poll();
            Outcome outcome = handle(player, source, next);
            if (outcome == Outcome.NO_ENERGY) {
                queue.clear();
                break;
            }
            if (outcome == Outcome.PLACED || outcome == Outcome.FAILED) {
                budget--;
            }
            if (requiresController && next.pos().equals(controllerPos) && outcome != Outcome.PLACED && outcome != Outcome.ALREADY) {
                controllerNotPlaced = true;
                queue.clear();
                break;
            }
        }
        return queue.isEmpty();
    }

    private Outcome handle(Player player, MaterialSource source, AssemblyPlanner.Planned next) {
        BlockPos pos = next.pos();
        if (!io.ticticboom.mods.mm.tool.ToolTarget.usablePosition(level, pos)) {
            blocked++;
            return Outcome.BLOCKED;
        }
        BlockState existing = level.getBlockState(pos);
        Block wanted = next.state().getBlock();
        if (existing.is(wanted) || next.accepted().contains(existing.getBlock())) {
            return Outcome.ALREADY;
        }
        if (!existing.isAir() && !existing.canBeReplaced()) {
            blocked++;
            return Outcome.BLOCKED;
        }
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            blocked++;
            return Outcome.BLOCKED;
        }
        if (!source.has(wanted)) {
            missing.merge(wanted, 1, Integer::sum);
            return Outcome.MISSING;
        }
        if (!source.payEnergy(perBlockFe)) {
            outOfEnergy = true;
            return Outcome.NO_ENERGY;
        }
        ItemStack taken = source.take(wanted);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, next.state(), Block.UPDATE_ALL)) {
            source.refund(taken);
            source.refundEnergy(perBlockFe);
            blocked++;
            return Outcome.FAILED;
        }
        if (EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore();
            source.refund(taken);
            source.refundEnergy(perBlockFe);
            blocked++;
            return Outcome.FAILED;
        }
        var sound = next.state().getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
        placed++;
        return Outcome.PLACED;
    }
}
