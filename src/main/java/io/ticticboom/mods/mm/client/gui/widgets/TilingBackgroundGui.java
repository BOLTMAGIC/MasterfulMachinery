package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.ScreenSizable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;

public class TilingBackgroundGui extends AbstractWidget implements ScreenSizable {
    private final int padding;
    private int width;
    private int height;
    public static final ResourceLocation guiTexture = Ref.UiTextures.TILING_GUI;

    public TilingBackgroundGui(int x, int y, int width, int height, int padding) {
        this.padding = padding;
        this.xPos = x;
        this.yPos = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blit(guiTexture, this.xPos, this.yPos, width,4, 4, 0, 18, 4, 64, 32);
        var leftX = this.xPos + this.padding;
        var topY = this.yPos + this.padding;

        // tl corner
        guiGraphics.blit(guiTexture, leftX, topY, 4, 4, 0, 0, 4, 4, 64, 32);
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

    @Override
    public void resize(int width, int height) {
        this.width = width - (this.padding * 2);
        this.height = height - (this.padding * 2);
    }
}
