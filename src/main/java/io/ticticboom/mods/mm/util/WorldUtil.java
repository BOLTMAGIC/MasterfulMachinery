package io.ticticboom.mods.mm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import static net.minecraft.world.level.block.Blocks.AIR;

/**
 * Utility class for faster world-related operations.
 */
public class WorldUtil {

    public static BlockState getBlockState(BlockPos pos, ServerLevel level) {
        if (level == null) {
            return AIR.defaultBlockState();
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkAccess chunk = getChunk(chunkX, chunkZ, level);

        if (chunk == null) {
            return AIR.defaultBlockState();
        }

        int sectionIndex = level.getSectionIndex(pos.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return AIR.defaultBlockState();
        }

        LevelChunkSection section = chunk.getSections()[sectionIndex];
        if (section == null || section.hasOnlyAir()) {
            return AIR.defaultBlockState();
        }

        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return section.getBlockState(localX, localY, localZ);
    }

    public static BlockEntity getBlockEntity(BlockPos pos, ServerLevel level) {
        if (level == null) {
            return null;
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkAccess chunk = getChunk(chunkX, chunkZ, level);

        if (chunk == null) {
            return null;
        }

        return chunk.getBlockEntity(pos);
    }

    public static ChunkAccess getChunk(int chunkX, int chunkZ, ServerLevel level) {
        return level.getChunkSource().getChunk(chunkX, chunkZ, true);
    }

    /**
     * Finds MachineControllerBlockEntity block entities inside a cube with edge length 2*radius+1 around pos.
     * Scans the corresponding chunks and returns the found block entities.
     */
    public static java.util.List<net.minecraft.world.level.block.entity.BlockEntity> findControllerBlockEntitiesInRadius(BlockPos pos, ServerLevel level, int radius) {
        var result = new java.util.ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
        if (level == null) return result;
        int minX = (pos.getX() - radius) >> 4;
        int maxX = (pos.getX() + radius) >> 4;
        int minZ = (pos.getZ() - radius) >> 4;
        int maxZ = (pos.getZ() + radius) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                ChunkAccess chunk = getChunk(cx, cz, level);
                if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk lc) {
                    for (var be : lc.getBlockEntities().values()) {
                        if (be instanceof io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity) {
                            // simple distance check to limit false positives
                            var cpos = be.getBlockPos();
                            if (Math.abs(cpos.getX() - pos.getX()) <= radius && Math.abs(cpos.getY() - pos.getY()) <= radius && Math.abs(cpos.getZ() - pos.getZ()) <= radius) {
                                result.add(be);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
