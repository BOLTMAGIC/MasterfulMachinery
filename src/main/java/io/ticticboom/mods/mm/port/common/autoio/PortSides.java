package io.ticticboom.mods.mm.port.common.autoio;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class PortSides {

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

    public static String nameKey(Direction world, @Nullable Direction front) {
        if (front == null) {
            return "gui.mm.port.side." + world.getName();
        }
        return "gui.mm.port.side.rel." + toRelative(world, front).name().toLowerCase(Locale.ROOT);
    }

    public static String shortKey(Direction world, @Nullable Direction front) {
        return nameKey(world, front) + ".short";
    }
}
