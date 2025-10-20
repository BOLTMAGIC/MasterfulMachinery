package io.ticticboom.mods.mm.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface IWidget extends Renderable, GuiEventListener, NarratableEntry {

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
    @Override
    default boolean isMouseOver(double pMouseX, double pMouseY) {
        return true;
    }
}
