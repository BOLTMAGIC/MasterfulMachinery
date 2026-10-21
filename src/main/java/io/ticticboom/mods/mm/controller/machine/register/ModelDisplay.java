package io.ticticboom.mods.mm.controller.machine.register;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * How a controller shows its GeckoLib model (geoModel), set in the controller screen for every player:
 * animated, standing still (animations off), or not drawn at all.
 */
public enum ModelDisplay {
    ON,
    OFF,
    HIDDEN;

    /** The next state for the screen button; a model without animations goes straight between ON and HIDDEN. */
    public ModelDisplay next(boolean hasAnimations) {
        return switch (this) {
            case ON -> hasAnimations ? OFF : HIDDEN;
            case OFF -> HIDDEN;
            case HIDDEN -> ON;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the state, or null for an unknown name */
    @Nullable
    public static ModelDisplay parse(String name) {
        for (ModelDisplay display : values()) {
            if (display.serializedName().equals(name)) {
                return display;
            }
        }
        return null;
    }
}
