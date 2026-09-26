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
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/** One running assembly: places planned blocks a few per tick, counting what could not be placed. */
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

    AssemblyJob(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.queue = new ArrayDeque<>(plan.steps());
        this.total = plan.steps().size();
        this.unavailable = plan.unavailable();
        this.perBlockFe = perBlockFe;
    }

    /** For game tests; players start jobs through {@link AssemblyJobs#start}. Controller path: 0 FE per block. */
    public static AssemblyJob create(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan) {
        return new AssemblyJob(level, controllerPos, plan, 0);
    }

    public static AssemblyJob create(ServerLevel level, BlockPos controllerPos, AssemblyPlanner.Plan plan, int perBlockFe) {
        return new AssemblyJob(level, controllerPos, plan, perBlockFe);
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

    /** Total planned positions, for "out of energy (x/y)" style reporting. */
    public int total() {
        return total;
    }

    /** True once the job stopped early because the source could not pay for the next block. */
    public boolean outOfEnergy() {
        return outOfEnergy;
    }

    public Map<Block, Integer> missing() {
        return Collections.unmodifiableMap(missing);
    }

    /** False once the controller was broken or replaced; the job should stop then. */
    public boolean controllerPresent() {
        return level.isLoaded(controllerPos) && level.getBlockEntity(controllerPos) instanceof MachineControllerBlockEntity;
    }

    /** @return true when every planned block has been handled (or the job stopped early, e.g. out of energy) */
    public boolean tick(Player player, MaterialSource source, int budget) {
        while (budget > 0 && !queue.isEmpty()) {
            AssemblyPlanner.Planned next = queue.poll();
            BlockPos pos = next.pos();
            BlockState existing = level.getBlockState(pos);
            Block wanted = next.state().getBlock();
            if (existing.is(wanted) || next.accepted().contains(existing.getBlock())) {
                continue; // already right (maybe another allowed tier), costs nothing
            }
            if (!existing.isAir() && !existing.canBeReplaced()) {
                blocked++;
                continue; // never break what the player built
            }
            if (!player.mayBuild() || !level.mayInteract(player, pos)) {
                blocked++;
                continue; // adventure mode, spawn protection, world border
            }
            if (!source.has(wanted)) {
                missing.merge(wanted, 1, Integer::sum);
                continue;
            }
            if (!source.payEnergy(perBlockFe)) {
                outOfEnergy = true;
                queue.clear();
                break; // remaining positions are left untouched, not counted missing
            }
            ItemStack taken = source.take(wanted);
            // placed like a player would: claim and protection mods may cancel it
            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            if (!level.setBlock(pos, next.state(), Block.UPDATE_ALL)) {
                source.refund(taken);
                blocked++;
                budget--;
                continue;
            }
            if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
                snapshot.restore(true, true);
                source.refund(taken);
                blocked++;
                budget--;
                continue;
            }
            var sound = next.state().getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
            placed++;
            budget--;
        }
        return queue.isEmpty();
    }
}
