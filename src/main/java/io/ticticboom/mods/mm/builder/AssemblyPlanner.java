package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Turns an MM structure into "put this block here" steps around an existing controller. */
public final class AssemblyPlanner {

    public record Planned(BlockPos pos, BlockState state) {
    }

    private AssemblyPlanner() {
    }

    /** Same mapping the blueprint uses for the player's facing; a controller faces where its placer looked. */
    public static Rotation rotationFor(Direction controllerFacing) {
        return switch (controllerFacing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * The rotation that already matches the most blocks around the controller, so a half-built machine is
     * finished the way it was started. With nothing built yet, the controller's facing decides.
     */
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

    public static List<Planned> plan(StructureModel model, BlockPos controllerPos, Rotation rotation, TierPrefs prefs, Predicate<Block> available) {
        var result = new ArrayList<Planned>();
        for (PositionedLayoutPiece positioned : pieces(model, rotation)) {
            Block block = chooseBlock(positioned.piece().piece(), prefs, available);
            if (block == null) {
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
            result.add(new Planned(pos, state));
        }
        return result;
    }

    /**
     * port_type positions follow the tier preference; other positions take the first candidate the
     * player can supply, else the first candidate.
     */
    public static @Nullable Block chooseBlock(StructurePiece piece, TierPrefs prefs, Predicate<Block> available) {
        if (piece instanceof PortTypeStructurePiece portType) {
            var byRank = portType.getBlocksByRank();
            if (byRank.isEmpty()) {
                return null;
            }
            String key = PortTiers.key(portType.getPortTypeId(), portType.getInput().orElse(true));
            int rank = TierResolver.resolve(prefs.get(key), portType.getMinTier(), portType.getMaxTier(), byRank.navigableKeySet());
            return rank < 0 ? null : byRank.get(rank);
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

    private static List<PositionedLayoutPiece> pieces(StructureModel model, Rotation rotation) {
        return model.layout().getRotatedPositionedPieces().getOrDefault(rotation, model.layout().getPositionedPieces());
    }
}
