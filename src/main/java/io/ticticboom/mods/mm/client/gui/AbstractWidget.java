package io.ticticboom.mods.mm.client.gui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

public class AbstractWidget implements IWidget {

    @Getter
    protected int xPos = 0;
    @Getter
    protected int yPos = 0;


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }
}
