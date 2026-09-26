package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Finds the machine a dismantle is aimed at and the blocks it is made of. */
public final class DismantlePlanner {
    /** How far from the aimed-at block a machine's controller may be. */
    public static final int SEARCH_RADIUS = 16;

    private DismantlePlanner() {
    }

    /**
     * The controller the aimed-at block belongs to: the block itself when it is a controller, else a controller
     * within {@link #SEARCH_RADIUS} whose formed structure contains it. Only loaded chunks are searched.
     */
    public static @Nullable MachineControllerBlockEntity resolve(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            return controller;
        }
        if (level.getBlockState(pos).isAir()) {
            return null;
        }
        MachineControllerBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = (pos.getX() - SEARCH_RADIUS) >> 4; cx <= (pos.getX() + SEARCH_RADIUS) >> 4; cx++) {
            for (int cz = (pos.getZ() - SEARCH_RADIUS) >> 4; cz <= (pos.getZ() + SEARCH_RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MachineControllerBlockEntity controller)) {
                        continue;
                    }
                    BlockPos cpos = be.getBlockPos();
                    if (Math.abs(cpos.getX() - pos.getX()) > SEARCH_RADIUS || Math.abs(cpos.getY() - pos.getY()) > SEARCH_RADIUS
                            || Math.abs(cpos.getZ() - pos.getZ()) > SEARCH_RADIUS) {
                        continue;
                    }
                    double dist = cpos.distSqr(pos);
                    if (dist < bestDist && structurePositions(level, controller).contains(pos)) {
                        best = controller;
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }

    /**
     * What dismantling this controller breaks: its formed structure's blocks (ports included), then the controller
     * last. An unformed controller is only itself. Air is left out.
     */
    public static List<BlockPos> positions(Level level, MachineControllerBlockEntity controller) {
        BlockPos controllerPos = controller.getBlockPos();
        Set<BlockPos> positions = new LinkedHashSet<>(structurePositions(level, controller));
        positions.remove(controllerPos);
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).isAir()) {
                result.add(pos.immutable());
            }
        }
        result.add(controllerPos);
        return result;
    }

    /** Every piece of the controller's cached structure (ports are pieces too), or none when it is not formed. */
    private static List<BlockPos> structurePositions(Level level, MachineControllerBlockEntity controller) {
        StructureModel structure = controller.getStructure();
        return structure == null ? List.of() : structure.getPositions(level, controller.getBlockPos());
    }
}
