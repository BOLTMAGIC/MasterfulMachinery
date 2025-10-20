package io.ticticboom.mods.mm.client.gui;

import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        for (IWidget child : children) {
            child.render(guiGraphics, mouseX, mouseY, partialTicks);
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
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        for (IWidget child : children) {
            child.mouseClicked(pMouseX, pMouseY, pButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        for (IWidget child : children) {
            child.mouseReleased(pMouseX, pMouseY, pButton);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        for (IWidget child : children) {
            child.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        for (IWidget child : children) {
            child.mouseScrolled(pMouseX, pMouseY, pDelta);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        for (IWidget child : children) {
            child.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        for (IWidget child : children) {
            child.keyReleased(pKeyCode, pScanCode, pModifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (IWidget child : children) {
            child.charTyped(pCodePoint, pModifiers);
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