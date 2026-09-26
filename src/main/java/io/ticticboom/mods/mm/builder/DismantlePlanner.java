package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
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

    /**
     * Client-safe preview of {@link #resolve} + {@link #positions} for outlining the aimed-at machine: never calls
     * the structure's formed checks (they need a server level). A structure counts as formed when, in its best
     * matching rotation, every piece holds one of its candidate blocks. The server stays authoritative.
     *
     * @return the machine's positions, controller last; empty when the block belongs to no machine
     */
    public static List<BlockPos> previewPositions(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            return previewPositions(level, controller);
        }
        if (level.getBlockState(pos).isAir()) {
            return List.of();
        }
        List<BlockPos> best = List.of();
        double bestDist = Double.MAX_VALUE;
        for (int cx = (pos.getX() - SEARCH_RADIUS) >> 4; cx <= (pos.getX() + SEARCH_RADIUS) >> 4; cx++) {
            for (int cz = (pos.getZ() - SEARCH_RADIUS) >> 4; cz <= (pos.getZ() + SEARCH_RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MachineControllerBlockEntity controller) || controller.getStructure() == null) {
                        continue;
                    }
                    BlockPos cpos = be.getBlockPos();
                    if (Math.abs(cpos.getX() - pos.getX()) > SEARCH_RADIUS || Math.abs(cpos.getY() - pos.getY()) > SEARCH_RADIUS
                            || Math.abs(cpos.getZ() - pos.getZ()) > SEARCH_RADIUS) {
                        continue;
                    }
                    double dist = cpos.distSqr(pos);
                    if (dist < bestDist) {
                        List<BlockPos> positions = previewPositions(level, controller);
                        if (positions.contains(pos)) {
                            best = positions;
                            bestDist = dist;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static List<BlockPos> previewPositions(Level level, MachineControllerBlockEntity controller) {
        BlockPos controllerPos = controller.getBlockPos();
        StructureModel structure = controller.getStructure();
        if (structure == null) {
            return List.of(controllerPos);
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        Direction facing = controllerState.hasProperty(HorizontalDirectionalBlock.FACING)
                ? controllerState.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        Rotation rotation = AssemblyPlanner.bestRotation(level, structure, controllerPos, facing);
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (PositionedLayoutPiece positioned : AssemblyPlanner.pieces(structure, rotation)) {
            BlockPos piecePos = positioned.findAbsolutePos(controllerPos);
            BlockState existing = level.getBlockState(piecePos);
            List<Block> candidates = positioned.piece().piece().createBlocksSupplier().get();
            if (existing.isAir() || candidates == null || !candidates.contains(existing.getBlock())) {
                // not formed: like the server, only the controller itself
                return List.of(controllerPos);
            }
            positions.add(piecePos.immutable());
        }
        positions.remove(controllerPos);
        List<BlockPos> result = new ArrayList<>(positions);
        result.add(controllerPos);
        return result;
    }

    /** Every piece of the controller's cached structure (ports are pieces too), or none when it is not formed. */
    private static List<BlockPos> structurePositions(Level level, MachineControllerBlockEntity controller) {
        StructureModel structure = controller.getStructure();
        return structure == null ? List.of() : structure.getPositions(level, controller.getBlockPos());
    }
}
