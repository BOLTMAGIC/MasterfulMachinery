package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
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
        var texture = Ref.UiTextures.BUTTON_ACTIVE;
        if (pressed) {
            texture = Ref.UiTextures.BUTTON_PRESSED;
        }
//        guiGraphics.blit(texturere, this.xPos, this.yPos, 0, 0f, 0f, 0, 0, 4,4, 64, 32);
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
        UP, DOWN
    }
}
