package io.ticticboom.mods.mm.client.gui;

import io.ticticboom.mods.mm.client.util.InputEvents;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWidget implements IWidget {

    @Getter
    @Setter
    protected GuiPos position;
    protected boolean focused = false;
    protected Minecraft mc = Minecraft.getInstance();
    protected List<IWidget> children = new ArrayList<>();

    protected <T extends IWidget> T addWidget(T widget) {
        children.add(widget);
        return widget;
    }

    public AbstractWidget(GuiPos pos) {
        this.position = pos;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        for (IWidget child : children) {
            child.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void setFocused(boolean pFocused) {
        focused = pFocused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        for (IWidget child : children) {
            child.mouseClicked(InputEvents.mouse(pMouseX, pMouseY, pButton), false);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        for (IWidget child : children) {
            child.mouseReleased(InputEvents.mouse(pMouseX, pMouseY, pButton));
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double pDragX, double pDragY) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        for (IWidget child : children) {
            child.mouseDragged(InputEvents.mouse(pMouseX, pMouseY, pButton), pDragX, pDragY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        for (IWidget child : children) {
            child.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int pKeyCode = event.key();
        int pScanCode = event.scancode();
        int pModifiers = event.modifiers();
        for (IWidget child : children) {
            child.keyPressed(new KeyEvent(pKeyCode, pScanCode, pModifiers));
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int pKeyCode = event.key();
        int pScanCode = event.scancode();
        int pModifiers = event.modifiers();
        for (IWidget child : children) {
            child.keyReleased(new KeyEvent(pKeyCode, pScanCode, pModifiers));
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char pCodePoint = (char) event.codepoint();
        int pModifiers = 0;
        for (IWidget child : children) {
            child.charTyped(new CharacterEvent(pCodePoint));
        }
        return false;
    }

    protected boolean mouseOver(double pMouseX, double pMouseY) {
        return this.position.contains((int)pMouseX, (int)pMouseY);
    }


    @Override
    @Deprecated(since = "Not enabled for use", forRemoval = true)
    public void mouseMoved(double pMouseX, double pMouseY) {
        IWidget.super.mouseMoved(pMouseX, pMouseY);
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return IWidget.super.isMouseOver(pMouseX, pMouseY);
    }
}