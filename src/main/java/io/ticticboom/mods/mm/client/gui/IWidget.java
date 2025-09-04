package io.ticticboom.mods.mm.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

public interface IWidget extends Renderable {

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
}
