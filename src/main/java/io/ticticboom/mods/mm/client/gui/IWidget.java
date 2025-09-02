package io.ticticboom.mods.mm.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public interface IWidget {
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
}
