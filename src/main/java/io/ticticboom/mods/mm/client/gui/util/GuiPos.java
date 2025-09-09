package io.ticticboom.mods.mm.client.gui.util;

public record GuiPos(GuiCoord coord, GuiSize size) {
    public static GuiPos of(GuiCoord coords, GuiSize size) {
        return new GuiPos(coords, size);
    }
    public static GuiPos of(int x, int y, int w, int h) {
        return new GuiPos(GuiCoord.of(x, y), GuiSize.of(w, h));
    }

    public int x() {
        return coord.x();
    }

    public int y() {
        return coord.y();
    }

    public int w() {
        return size.w();
    }

    public int h() {
        return size.h();
    }
}
