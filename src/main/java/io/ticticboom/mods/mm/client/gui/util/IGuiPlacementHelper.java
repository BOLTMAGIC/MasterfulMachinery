package io.ticticboom.mods.mm.client.gui.util;

import org.joml.Vector2i;

public interface IGuiPlacementHelper {
    int getGuiLeft();
    int getGuiTop();
    int getGuiRight();
    int getGuiBottom();
    int getGuiWidth();
    int getGuiHeight();
    int getGuiCenterX();
    int getGuiCenterY();
    int getContainerWidth();
    int getContainerHeight();
    int getContainerPadding();
    GuiBounds getGuiBounds();
    GuiPos getGuiPos();
    Vector2i offset(GuiAlignment alignment, Vector2i offset);
    default GuiCoord offset(GuiAlignment alignment, GuiCoord offset) {
        return GuiCoord.of(offset(alignment, new Vector2i(offset.x(), offset.y())));
    }

    default int fromBottom(int y) {
        return (getGuiBottom() - y) - getContainerPadding();
    }

    default int fromBottom(int y, int subOffset) {
        return fromBottom(y) - subOffset;
    }

    default int columnWidth(int numColumns) {
        return getGuiRight() / numColumns;
    }

    default int fromRight(int x) {
        return getGuiRight() - x - getContainerPadding();
    }

    GuiPos offset(GuiAlignment alignment, GuiPos pos);
}
