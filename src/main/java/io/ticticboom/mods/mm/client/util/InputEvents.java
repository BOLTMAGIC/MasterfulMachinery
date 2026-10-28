package io.ticticboom.mods.mm.client.util;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/** Mouse events for forwarding a click to a child widget at other coordinates (input is event objects since 1.21.9). */
public final class InputEvents {
    private InputEvents() {
    }

    public static MouseButtonEvent mouse(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }
}
