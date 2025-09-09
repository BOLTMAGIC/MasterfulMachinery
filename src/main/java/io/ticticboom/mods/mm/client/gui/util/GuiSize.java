package io.ticticboom.mods.mm.client.gui.util;

public record GuiSize(int w, int h) {
    public static GuiSize of(int w, int h) {
        return new GuiSize(w, h);
    }
}
