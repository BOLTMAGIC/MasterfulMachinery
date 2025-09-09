package io.ticticboom.mods.mm.client.gui.util;

import org.joml.Vector2i;

public record GuiCoord(int x, int y) {
    public static GuiCoord of(int x, int y) {
        return new GuiCoord(x, y);
    }

    public static GuiCoord of(Vector2i pos) {
        return new GuiCoord(pos.x, pos.y);
    }
}
