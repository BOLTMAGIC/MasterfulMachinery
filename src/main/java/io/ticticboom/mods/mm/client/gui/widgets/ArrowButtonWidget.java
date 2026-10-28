package io.ticticboom.mods.mm.client.gui.widgets;

import net.minecraft.client.input.MouseButtonEvent;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.GuiClickEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ArrowButtonWidget extends AbstractWidget {
    private final ArrowDirection direction;
    private boolean pressed = false;
    public final GuiEventHandler<GuiClickEvent> clickEmitter = new GuiEventHandler<>();

    public ArrowButtonWidget(GuiPos pos, ArrowDirection direction) {
        super(pos);
        this.direction = direction;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        if (mouseOver(pMouseX, pMouseY)) {
            this.clickEmitter.fireEvent(new GuiClickEvent((int)pMouseX, (int)pMouseY));
            pressed = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        pressed = false;
        return super.mouseReleased(event);
    }
}
