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
        var result = false;
        for (IWidget child : children) {
            if (child.mouseClicked(pMouseX, pMouseY, pButton)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        var result = false;
        for (IWidget child : children) {
            if (child.mouseReleased(pMouseX, pMouseY, pButton)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        var result = false;
        for (IWidget child : children) {
            if (child.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        var result = false;
        for (IWidget child : children) {
            if (child.mouseScrolled(pMouseX, pMouseY, pDelta)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        var result = false;
        for (IWidget child : children) {
            if (child.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        var result = false;
        for (IWidget child : children) {
            if (child.keyReleased(pKeyCode, pScanCode, pModifiers)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        var result = false;
        for (IWidget child : children) {
            if (child.charTyped(pCodePoint, pModifiers)) {
                result = true;
            }
        }
        return result;
    }

    protected boolean mouseOver(double pMouseX, double pMouseY) {
        return this.position.contains((int)pMouseX, (int)pMouseY);
    }


    @Override
    @Deprecated(since = "Not enabled for use", forRemoval = true)
    public void mouseMoved(double pMouseX, double pMouseY) {
        IWidget.super.mouseMoved(pMouseX, pMouseY);
    }
}