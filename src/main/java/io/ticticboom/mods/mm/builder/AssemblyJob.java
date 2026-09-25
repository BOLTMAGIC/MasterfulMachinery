package io.ticticboom.mods.mm.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One running assembly: places planned blocks a few per tick, counting what could not be placed. */
public final class AssemblyJob {
    final ServerLevel level;
    final BlockPos controllerPos;
    private final Deque<AssemblyPlanner.Planned> queue;
    int placed;
    int blocked;
    final Map<Block, Integer> missing = new LinkedHashMap<>();

    AssemblyJob(ServerLevel level, BlockPos controllerPos, List<AssemblyPlanner.Planned> plan) {
        this.level = level;
        this.controllerPos = controllerPos;
        this.queue = new ArrayDeque<>(plan);
    }

    /** @return true when every planned block has been handled */
    boolean tick(Player player, int budget) {
        while (budget > 0 && !queue.isEmpty()) {
            AssemblyPlanner.Planned next = queue.poll();
            BlockState existing = level.getBlockState(next.pos());
            Block wanted = next.state().getBlock();
            if (existing.is(wanted)) {
                continue; // already right, costs nothing
            }
            if (!existing.isAir() && !existing.canBeReplaced()) {
                blocked++;
                continue; // never break what the player built
            }
            if (!PlayerMaterials.take(player, wanted)) {
                missing.merge(wanted, 1, Integer::sum);
                continue;
            }
            level.setBlock(next.pos(), next.state(), Block.UPDATE_ALL);
            var sound = next.state().getSoundType();
            level.playSound(null, next.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
            placed++;
            budget--;
        }
        return queue.isEmpty();
    }
}
