package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import net.minecraft.client.gui.GuiGraphics;

public class ArrowButtonWidget extends AbstractWidget {
    private final ArrowDirection direction;
    private boolean pressed = false;

    public ArrowButtonWidget(GuiPos pos, ArrowDirection direction) {
        super(pos);
        this.direction = direction;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (pressed) {
            GuiTextures.BUTTON_PRESSED.blit(guiGraphics, position.x(), position.y());
        } else {
            GuiTextures.BUTTON_ACTIVE.blit(guiGraphics, position.x(), position.y());
        }

        if (this.direction == ArrowDirection.LEFT) {
            GuiTextures.ARROW_LEFT.blit(guiGraphics, position.x(), position.y());
        } else {
            GuiTextures.ARROW_RIGHT.blit(guiGraphics, position.x(), position.y());
        }
    }

    public enum ArrowDirection {
        LEFT,
        RIGHT;
    }
}
