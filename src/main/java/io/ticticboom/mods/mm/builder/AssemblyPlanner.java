package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class AssemblyPlanner {

    public record Planned(BlockPos pos, BlockState state, List<Block> accepted) {
    }

    public record Plan(List<Planned> steps, int unavailable) {
    }

    private AssemblyPlanner() {
    }

    public static Rotation rotationFor(Direction controllerFacing) {
        return switch (controllerFacing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static Rotation bestRotation(Level level, StructureModel model, BlockPos controllerPos, Direction controllerFacing) {
        Rotation best = rotationFor(controllerFacing);
        int bestScore = matching(level, model, controllerPos, best);
        for (Rotation rotation : Rotation.values()) {
            int score = matching(level, model, controllerPos, rotation);
            if (score > bestScore) {
                best = rotation;
                bestScore = score;
            }
        }
        return best;
    }

    private static int matching(Level level, StructureModel model, BlockPos controllerPos, Rotation rotation) {
        int count = 0;
        for (PositionedLayoutPiece positioned : pieces(model, rotation)) {
            Block existing = level.getBlockState(positioned.findAbsolutePos(controllerPos)).getBlock();
            List<Block> candidates = positioned.piece().piece().createBlocksSupplier().get();
            if (candidates != null && candidates.contains(existing)) {
                count++;
            }
        }
        return count;
    }

    public static @Nullable Plan planCompletion(Level level, StructureModel model, BlockPos controllerPos, TierPrefs prefs, Predicate<Block> available) {
        if (model.formed(level, controllerPos)) {
            return null;
        }
        Direction facing = level.getBlockState(controllerPos).getValue(HorizontalDirectionalBlock.FACING);
        Rotation rotation = bestRotation(level, model, controllerPos, facing);
        return plan(model, controllerPos, rotation, prefs, available);
    }

    public static Plan plan(StructureModel model, BlockPos controllerPos, Rotation rotation, TierPrefs prefs, Predicate<Block> available) {
        var steps = new ArrayList<Planned>();
        int unavailable = 0;
        for (PositionedLayoutPiece positioned : pieces(model, rotation)) {
            StructurePiece piece = positioned.piece().piece();
            Block block = chooseBlock(piece, prefs, available);
            if (block == null) {
                unavailable++;
                continue;
            }
            BlockPos pos = positioned.findAbsolutePos(controllerPos);
            BlockState state = block.defaultBlockState().rotate(rotation);
            List<StructurePieceModifier> modifiers = positioned.piece().modifiers();
            if (modifiers != null) {
                for (StructurePieceModifier modifier : modifiers) {
                    state = modifier.modifyBlockState(state, null, pos);
                }
            }
            List<Block> candidates = piece.createBlocksSupplier().get();
            List<Block> accepted = candidates == null ? List.of() : candidates.stream().filter(Objects::nonNull).toList();
            steps.add(new Planned(pos, state, accepted));
        }
        return new Plan(steps, unavailable);
    }

    public static @Nullable Block chooseBlock(StructurePiece piece, TierPrefs prefs, Predicate<Block> available) {
        if (piece instanceof TieredPortPiece tiered) {
            int rank = tiered.resolveTier(prefs.get(tiered.tierKey()));
            return rank < 0 ? null : tiered.getBlocksByRank().get(rank);
        }
        List<Block> candidates = piece.createBlocksSupplier().get();
        if (candidates == null) {
            return null;
        }
        List<Block> nonNull = candidates.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        return nonNull.stream().filter(available).findFirst().orElse(nonNull.get(0));
    }

    static List<PositionedLayoutPiece> pieces(StructureModel model, Rotation rotation) {
        return model.layout().getRotatedPositionedPieces().getOrDefault(rotation, model.layout().getPositionedPieces());
    }
}
