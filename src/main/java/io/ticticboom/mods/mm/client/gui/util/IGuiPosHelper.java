package io.ticticboom.mods.mm.client.gui.util;

import org.joml.Vector2i;

public interface IGuiPosHelper {
    int getGuiLeft();
    int getGuiTop();
    int getGuiRight();
    int getGuiBottom();
    int getGuiWidth();
    int getGuiHeight();
    int getGuiCenterX();
    int getGuiCenterY();
    int getScreenWidth();
    int getScreenHeight();
    int getScreenPadding();
    GuiBounds getGuiBounds();
    GuiPos getGuiPos();
    Vector2i offset(GuiAlignment alignment, Vector2i offset);
    default GuiCoord offset(GuiAlignment alignment, GuiCoord offset) {
        return GuiCoord.of(offset(alignment, new Vector2i(offset.x(), offset.y())));
    }

    GuiPos offset(GuiAlignment alignment, GuiPos pos);
}
