package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ArrowButtonWidget extends AbstractWidget {
    private final ArrowDirection direction;
    private boolean pressed = false;

    public ArrowButtonWidget(ArrowDirection direction) {
        this.direction = direction;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (pressed) {
            GuiTextures.BUTTON_PRESSED.blit(guiGraphics, xPos, yPos);
        } else {
            GuiTextures.BUTTON_ACTIVE.blit(guiGraphics, xPos, yPos);
        }
        if (this.direction == ArrowDirection.LEFT) {

        }
    }

    @Override
    public void setFocused(boolean pFocused) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    public enum ArrowDirection {
        LEFT,
        RIGHT;
    }
}
