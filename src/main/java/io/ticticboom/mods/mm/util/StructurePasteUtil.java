package io.ticticboom.mods.mm.util;

import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Small, conservative helper for creative-only structure pasting from blueprint items.
 *
 * This intentionally does not try to be a full schematic system. It places the first
 * possible block candidate for each structure piece, refuses to overwrite solid blocks,
 * and leaves normal survival gameplay untouched.
 */
public final class StructurePasteUtil {
    private static final int MAX_OBSTRUCTION_EXAMPLES = 5;

    private StructurePasteUtil() {
    }

    public static PasteResult paste(ServerLevel level, Player player, StructureModel model, BlockPos clickedPos, Direction clickedFace, Rotation rotation, Direction frontDirection) {
        PastePlan pastePlan = createPlanForPlacementAnchor(model, clickedPos.relative(clickedFace), rotation, frontDirection);
        List<PlannedBlock> plan = pastePlan.blocks();
        if (plan.isEmpty()) {
            return PasteResult.failure(Component.literal("Blueprint paste failed: structure has no placeable blocks."));
        }

        List<BlockPos> obstructed = findObstructions(level, plan);
        if (!obstructed.isEmpty()) {
            return PasteResult.failure(obstructionMessage(obstructed));
        }

        for (PlannedBlock planned : plan) {
            level.setBlock(planned.pos(), planned.state(), Block.UPDATE_ALL);
        }

        return PasteResult.success(Component.literal("Pasted structure: " + model.name() + " (" + plan.size() + " blocks)"));
    }

    /**
     * Creates a paste plan anchored by the block the player is pointing at.
     *
     * The clicked face contributes the anchor block ({@code clickedPos.relative(face)}),
     * while the player's horizontal facing defines the machine front. The full structure is
     * shifted so the anchor block represents the bottom-middle-front position of the final
     * multiblock bounding box. This makes floor placement intuitive and prevents side
     * placement from using the controller as a hidden center point.
     */
    public static PastePlan createPlanForPlacementAnchor(StructureModel model, BlockPos anchorPos, Rotation rotation, Direction frontDirection) {
        BlockPos controllerPos = anchorPos;
        List<PlannedBlock> initialPlan = createPlan(model, controllerPos, rotation);
        if (initialPlan.isEmpty()) {
            return new PastePlan(controllerPos, initialPlan);
        }

        BlockPos offset = offsetBottomMiddleFrontToAnchor(anchorPos, frontDirection, bounds(initialPlan));
        if (!offset.equals(BlockPos.ZERO)) {
            controllerPos = controllerPos.offset(offset);
        }

        return new PastePlan(controllerPos, createPlan(model, controllerPos, rotation));
    }

    public static List<PlannedBlock> createPlan(StructureModel model, BlockPos controllerPos, Rotation rotation) {
        var plan = new ArrayList<PlannedBlock>();

        Block controllerBlock = findControllerBlock(model);
        if (controllerBlock != null) {
            plan.add(new PlannedBlock(controllerPos, controllerBlock.defaultBlockState().rotate(rotation)));
        }

        Map<Rotation, List<PositionedLayoutPiece>> rotatedPieces = model.layout().getRotatedPositionedPieces();
        List<PositionedLayoutPiece> pieces = rotatedPieces.getOrDefault(rotation, model.layout().getPositionedPieces());
        for (PositionedLayoutPiece positioned : pieces) {
            BlockPos pos = positioned.findAbsolutePos(controllerPos);
            BlockState state = createBlockState(positioned.piece(), pos, rotation);
            if (state != null) {
                plan.add(new PlannedBlock(pos, state));
            }
        }

        return plan;
    }

    private static Block findControllerBlock(StructureModel model) {
        for (ResourceLocation controllerId : model.controllerIds().getIds()) {
            Block block = MMControllerRegistry.getControllerBlock(controllerId);
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    private static BlockState createBlockState(StructureLayoutPiece layoutPiece, BlockPos pos, Rotation rotation) {
        List<Block> blocks = layoutPiece.piece().createBlocksSupplier().get();
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }

        Block block = blocks.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (block == null) {
            return null;
        }

        BlockState state = block.defaultBlockState().rotate(rotation);
        List<StructurePieceModifier> modifiers = layoutPiece.modifiers();
        if (modifiers != null) {
            for (StructurePieceModifier modifier : modifiers) {
                state = modifier.modifyBlockState(state, null, pos);
            }
        }
        return state;
    }

    private static List<BlockPos> findObstructions(ServerLevel level, List<PlannedBlock> plan) {
        var obstructed = new ArrayList<BlockPos>();
        for (PlannedBlock planned : plan) {
            if (canPlaceAt(level, planned)) {
                continue;
            }
            obstructed.add(planned.pos());
        }
        return obstructed;
    }

    public static boolean canPlaceAt(Level level, PlannedBlock planned) {
        BlockState existing = level.getBlockState(planned.pos());
        return existing.isAir() || existing.canBeReplaced() || existing.is(planned.state().getBlock());
    }

    public static AABB bounds(List<PlannedBlock> plan) {
        AABB bounds = new AABB(plan.get(0).pos());
        for (int i = 1; i < plan.size(); i++) {
            bounds = bounds.minmax(new AABB(plan.get(i).pos()));
        }
        return bounds;
    }

    private static BlockPos offsetBottomMiddleFrontToAnchor(BlockPos anchorPos, Direction frontDirection, AABB bounds) {
        Direction.Axis frontAxis = frontDirection.getAxis();
        Direction.Axis sideAxis = frontAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

        int dx = 0;
        int dy = anchorPos.getY() - minCoordinate(bounds, Direction.Axis.Y);
        int dz = 0;

        int frontDelta = frontOffset(anchorPos, frontDirection, bounds);
        int sideDelta = axisCoordinate(anchorPos, sideAxis) - middleCoordinate(bounds, sideAxis);

        if (frontAxis == Direction.Axis.X) {
            dx = frontDelta;
            dz = sideDelta;
        } else {
            dz = frontDelta;
            dx = sideDelta;
        }

        return new BlockPos(dx, dy, dz);
    }

    private static int frontOffset(BlockPos anchorPos, Direction frontDirection, AABB bounds) {
        Direction.Axis axis = frontDirection.getAxis();
        int anchor = axisCoordinate(anchorPos, axis);

        // The placement anchor is meant to be the near/front bottom-middle block from
        // the player's point of view. The initial implementation used the opposite
        // face of the rotated bounds, which made the clicked anchor land on the far
        // side of the machine. Select the near face instead so the structure extends
        // away from the player/anchor rather than behind it.
        if (frontDirection.getAxisDirection().getStep() > 0) {
            return anchor - minCoordinate(bounds, axis);
        }
        return anchor - maxCoordinate(bounds, axis);
    }

    private static int axisCoordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static int minCoordinate(AABB bounds, Direction.Axis axis) {
        return switch (axis) {
            case X -> (int) Math.floor(bounds.minX);
            case Y -> (int) Math.floor(bounds.minY);
            case Z -> (int) Math.floor(bounds.minZ);
        };
    }

    private static int maxCoordinate(AABB bounds, Direction.Axis axis) {
        return switch (axis) {
            case X -> (int) Math.floor(bounds.maxX - 1.0E-7D);
            case Y -> (int) Math.floor(bounds.maxY - 1.0E-7D);
            case Z -> (int) Math.floor(bounds.maxZ - 1.0E-7D);
        };
    }

    private static int middleCoordinate(AABB bounds, Direction.Axis axis) {
        int min = minCoordinate(bounds, axis);
        int max = maxCoordinate(bounds, axis);
        return min + ((max - min) / 2);
    }

    private static Component obstructionMessage(List<BlockPos> obstructed) {
        var message = new StringBuilder("Blueprint paste blocked by ").append(obstructed.size()).append(" occupied position");
        if (obstructed.size() != 1) {
            message.append('s');
        }
        message.append(": ");

        int shown = Math.min(MAX_OBSTRUCTION_EXAMPLES, obstructed.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                message.append(", ");
            }
            BlockPos pos = obstructed.get(i);
            message.append(pos.getX()).append(' ').append(pos.getY()).append(' ').append(pos.getZ());
        }
        if (obstructed.size() > shown) {
            message.append(", ...");
        }
        return Component.literal(message.toString());
    }

    public record PastePlan(BlockPos controllerPos, List<PlannedBlock> blocks) {
    }

    public record PlannedBlock(BlockPos pos, BlockState state) {
    }

    public record PasteResult(boolean success, Component message) {
        public static PasteResult success(Component message) {
            return new PasteResult(true, message);
        }

        public static PasteResult failure(Component message) {
            return new PasteResult(false, message);
        }
    }
}
