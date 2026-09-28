package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.piece.type.port.PortAnywhereStructurePiece;
import io.ticticboom.mods.mm.piece.type.port.PortStructurePiece;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeAnywhereStructurePiece;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds the machine a dismantle is aimed at and the blocks it is made of. */
public final class DismantlePlanner {
    /** How far from the aimed-at block a machine's controller may be. */
    public static final int SEARCH_RADIUS = 16;

    private DismantlePlanner() {
    }

    /** A block to dismantle and what stood there when the dismantle was planned. */
    public record Target(BlockPos pos, Block block) {
    }

    /** A complete match for the selected, controller-free structure. The center block is kept last for previews. */
    public record BuilderMatch(BlockPos center, List<Target> positions) {
    }

    private record Candidate(Rotation rotation, BlockPos origin) {
    }

    /**
     * Find a complete copy of the selected builder structure containing the clicked block. The file's rotation and
     * original placement point are not stored in the world, so try only translations that place a matching block at
     * the clicked position. Matching block types rather than full states permits stairs and formed machines to update
     * their states after placement. An incomplete or ambiguous match is never dismantled.
     */
    public static @Nullable BuilderMatch matchBuilder(Level level, BlockPos clicked, ItemStack tool) {
        var id = ToolData.builderStructure(tool);
        if (id == null || !level.isLoaded(clicked)) return null;
        BuildableStructure structure = (level.isClientSide ? BuildableStructureRegistry.CLIENT : BuildableStructureRegistry.SERVER).get(id);
        if (structure == null || structure.blocks().isEmpty()) return null;
        Block clickedBlock = level.getBlockState(clicked).getBlock();
        Map<Block, Integer> frequency = new HashMap<>();
        for (BuildableStructure.Placement placement : structure.blocks()) {
            frequency.merge(placement.state().getBlock(), 1, Integer::sum);
        }
        // Rare blocks reject incorrect translations early, before checking a large casing wall.
        List<BuildableStructure.Placement> probes = new ArrayList<>(structure.blocks());
        probes.sort(Comparator.comparingInt(placement -> frequency.get(placement.state().getBlock())));
        Set<Candidate> tried = new HashSet<>();
        Set<BlockPos> matchedPositions = null;
        int checks = 0;
        for (Rotation rotation : Rotation.values()) {
            for (BuildableStructure.Placement clickedPiece : structure.blocks()) {
                if (clickedPiece.state().getBlock() != clickedBlock) continue;
                BlockPos origin = clicked.subtract(clickedPiece.pos().rotate(rotation));
                if (!tried.add(new Candidate(rotation, origin))) continue;
                Set<BlockPos> candidate = new LinkedHashSet<>();
                boolean complete = true;
                for (BuildableStructure.Placement probe : probes) {
                    if (++checks > 250_000) return null;
                    BlockPos pos = origin.offset(probe.pos().rotate(rotation));
                    if (!level.isLoaded(pos) || !level.getBlockState(pos).is(probe.state().getBlock())) {
                        complete = false;
                        break;
                    }
                    candidate.add(pos.immutable());
                }
                if (!complete) continue;
                if (matchedPositions != null && !matchedPositions.equals(candidate)) return null;
                matchedPositions = candidate;
            }
        }
        if (matchedPositions == null) return null;
        List<BlockPos> ordered = new ArrayList<>(matchedPositions);
        // Remove upper blocks first so gravity and support updates do not drop pieces before their turn.
        ordered.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        int minX = ordered.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int maxX = ordered.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int minY = ordered.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int maxY = ordered.stream().mapToInt(BlockPos::getY).max().orElseThrow();
        int minZ = ordered.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int maxZ = ordered.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        BlockPos middle = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        BlockPos center = ordered.stream().min(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(middle))
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).orElseThrow();
        ordered.remove(center);
        ordered.add(center);
        List<Target> targets = ordered.stream().map(pos -> new Target(pos, level.getBlockState(pos).getBlock())).toList();
        return new BuilderMatch(center, targets);
    }

    /** MM controllers first, then a complete match for the selected controller-free structure. */
    public static List<BlockPos> previewPositions(Level level, BlockPos pos, ItemStack tool) {
        List<BlockPos> mm = previewPositions(level, pos);
        if (!mm.isEmpty()) return mm;
        BuilderMatch builder = matchBuilder(level, pos, tool);
        return builder == null ? List.of() : builder.positions().stream().map(Target::pos).toList();
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
     * last. An unformed controller is only itself. Air is left out. Each target keeps the block standing there now,
     * so a block swapped in later is not broken.
     */
    public static List<Target> positions(Level level, MachineControllerBlockEntity controller) {
        BlockPos controllerPos = controller.getBlockPos();
        Set<BlockPos> positions = new LinkedHashSet<>(structurePositions(level, controller));
        positions.remove(controllerPos);
        List<Target> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                result.add(new Target(pos.immutable(), state.getBlock()));
            }
        }
        result.add(new Target(controllerPos, level.getBlockState(controllerPos).getBlock()));
        return result;
    }

    /**
     * Client-safe preview of {@link #resolve} + {@link #positions} for outlining the aimed-at machine: never calls
     * the structure's formed checks (they need a server level). Uses the controller's synced formed flag and the
     * best matching rotation; without the flag, every piece must hold a fitting block. The server stays
     * authoritative.
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
        Rotation rotation = AssemblyPlanner.rotationFor(facing);
        int bestScore = previewMatches(level, structure, controllerPos, rotation);
        for (Rotation candidate : Rotation.values()) {
            int score = previewMatches(level, structure, controllerPos, candidate);
            if (score > bestScore) {
                rotation = candidate;
                bestScore = score;
            }
        }
        List<PositionedLayoutPiece> pieces = AssemblyPlanner.pieces(structure, rotation);
        // the server's formed flag is synced; the own check is only a fallback for a controller not validated yet
        if (!controller.isFormed() && bestScore < pieces.size()) {
            // not formed: like the server, only the controller itself
            return List.of(controllerPos);
        }
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (PositionedLayoutPiece positioned : pieces) {
            BlockPos piecePos = positioned.findAbsolutePos(controllerPos);
            if (!level.getBlockState(piecePos).isAir()) {
                positions.add(piecePos.immutable());
            }
        }
        positions.remove(controllerPos);
        List<BlockPos> result = new ArrayList<>(positions);
        result.add(controllerPos);
        return result;
    }

    /**
     * How many pieces hold a fitting block in this rotation, as loosely as the server's formed check: a port that
     * may go anywhere fits any anywhere-port candidate, and an input gateway stands in for a non-port piece.
     */
    private static int previewMatches(Level level, StructureModel structure, BlockPos controllerPos, Rotation rotation) {
        StructureLayout layout = structure.layout();
        List<PositionedLayoutPiece> pieces = AssemblyPlanner.pieces(structure, rotation);
        Set<Block> anywhere = new HashSet<>();
        for (PositionedLayoutPiece positioned : pieces) {
            if (layout.isAnywhere(positioned.piece().piece())) {
                List<Block> candidates = positioned.piece().piece().createBlocksSupplier().get();
                if (candidates != null) {
                    anywhere.addAll(candidates);
                }
            }
        }
        Block gateway = MMRegisters.INPUT_GATEWAY.get();
        int count = 0;
        for (PositionedLayoutPiece positioned : pieces) {
            StructurePiece piece = positioned.piece().piece();
            Block existing = level.getBlockState(positioned.findAbsolutePos(controllerPos)).getBlock();
            List<Block> candidates = piece.createBlocksSupplier().get();
            boolean fits;
            if (layout.isAnywhere(piece)) {
                fits = anywhere.contains(existing);
            } else if (isPort(piece)) {
                fits = candidates != null && candidates.contains(existing);
            } else {
                fits = existing == gateway || (candidates != null && candidates.contains(existing));
            }
            if (fits) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPort(StructurePiece piece) {
        return piece instanceof PortStructurePiece || piece instanceof PortTypeStructurePiece
                || piece instanceof PortAnywhereStructurePiece || piece instanceof PortTypeAnywhereStructurePiece;
    }

    /** Every piece of the controller's cached structure (ports are pieces too), or none when it is not formed. */
    private static List<BlockPos> structurePositions(Level level, MachineControllerBlockEntity controller) {
        StructureModel structure = controller.getStructure();
        return structure == null ? List.of() : structure.getPositions(level, controller.getBlockPos());
    }
}
