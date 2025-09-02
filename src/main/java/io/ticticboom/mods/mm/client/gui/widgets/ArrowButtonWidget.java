package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;

public class ArrowButtonWidget extends AbstractWidget {
    private final ArrowDirection direction;

    public ArrowButtonWidget(ArrowDirection direction) {
        this.direction = direction;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

    }

    public static enum ArrowDirection {
        UP, DOWN
    }
}
