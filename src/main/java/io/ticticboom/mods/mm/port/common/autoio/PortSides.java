package io.ticticboom.mods.mm.port.common.autoio;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Port sides named relative to the multiblock's controller (front = the controller's front face,
 * left/right as seen by a player standing in front of it). Ports that aren't part of a formed
 * multiblock fall back to compass directions.
 */
public final class PortSides {
    private static final int CONTROLLER_SEARCH_RADIUS = 16;

    public enum Relative {
        TOP, BOTTOM, FRONT, BACK, LEFT, RIGHT
    }

    private PortSides() {
    }

    public static Direction toWorld(Relative side, Direction front) {
        return switch (side) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> front;
            case BACK -> front.getOpposite();
            // a player standing in front of the controller faces front.getOpposite()
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
        };
    }

    public static Relative toRelative(Direction world, Direction front) {
        for (Relative side : Relative.values()) {
            if (toWorld(side, front) == world) {
                return side;
            }
        }
        throw new IllegalArgumentException("front must be horizontal: " + front);
    }

    /**
     * @param front the controller's front, or null to use compass names
     */
    public static String nameKey(Direction world, @Nullable Direction front) {
        if (front == null) {
            return "gui.mm.port.side." + world.getName();
        }
        return "gui.mm.port.side.rel." + toRelative(world, front).name().toLowerCase();
    }

    public static String shortKey(Direction world, @Nullable Direction front) {
        return nameKey(world, front) + ".short";
    }

    /**
     * Finds the front face of the formed multiblock the port belongs to. Only loaded chunks are
     * searched and ports are matched by storage identity.
     *
     * @return the horizontal direction the controller's front faces, or null if the port isn't part of a formed multiblock
     */
    @Nullable
    public static Direction findControllerFront(ServerLevel level, BlockPos portPos, IPortStorage storage) {
        int r = CONTROLLER_SEARCH_RADIUS;
        for (int cx = (portPos.getX() - r) >> 4; cx <= (portPos.getX() + r) >> 4; cx++) {
            for (int cz = (portPos.getZ() - r) >> 4; cz <= (portPos.getZ() + r) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof MachineControllerBlockEntity controller
                            && be.getBlockPos().closerThan(portPos, r + 1)
                            && belongsTo(level, controller, storage)) {
                        var state = controller.getBlockState();
                        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                            return null;
                        }
                        // the controller model's front overlay faces opposite to FACING
                        return state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
                    }
                }
            }
        }
        return null;
    }

    private static boolean belongsTo(ServerLevel level, MachineControllerBlockEntity controller, IPortStorage storage) {
        var structure = controller.getStructure();
        if (structure == null) {
            return false;
        }
        var storages = structure.getStorages(level, controller.getBlockPos());
        if (storages == null) {
            return false;
        }
        for (IPortStorage s : storages.inputStorages()) {
            if (s == storage) return true;
        }
        for (IPortStorage s : storages.outputStorages()) {
            if (s == storage) return true;
        }
        return false;
    }
}
