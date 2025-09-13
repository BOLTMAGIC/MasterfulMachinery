package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.GuiClickEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import net.minecraft.client.gui.GuiGraphics;

public class ArrowButtonWidget extends AbstractWidget {
    private final ArrowDirection direction;
    private boolean pressed = false;
    public final GuiEventHandler<GuiClickEvent> clickEmitter = new GuiEventHandler<>();

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

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        int mouseX = (int)pMouseX;
        int mouseY = (int)pMouseY;
        if (this.position.contains(mouseX, mouseY)) {
            this.clickEmitter.fireEvent(new GuiClickEvent(mouseX, mouseY));
            return true;
        }
        return false;
    }
}
