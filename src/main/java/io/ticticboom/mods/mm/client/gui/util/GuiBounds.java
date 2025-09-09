package io.ticticboom.mods.mm.client.gui.util;

public record GuiBounds(int nearX, int nearY, int farX, int farY, int width, int height) {
    public static GuiBounds of(int nearX, int nearY, int farX, int farY) {
        return new GuiBounds(nearX, nearY, farX, farY, farX - nearX, farY - nearY);
    }

    public static GuiBounds of(GuiCoord pos, GuiSize size) {
        return new GuiBounds(pos.x(), pos.y(), pos.x() + size.w(), pos.y() + size.h(), size.w(), size.h());
    }
}
