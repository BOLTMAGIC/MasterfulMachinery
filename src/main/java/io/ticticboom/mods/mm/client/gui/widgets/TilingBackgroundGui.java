package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TilingBackgroundGui extends AbstractWidget {
    public static final ResourceLocation guiTexture = Ref.UiTextures.TILING_GUI;
    private final int padding;
    private final int width;
    private final int height;
    private final int tileSize = 4;

    public TilingBackgroundGui(int x, int y, int width, int height, int padding) {
        this.padding = padding;
        this.xPos = x;
        this.yPos = y;
        this.width = (width - (this.padding * 2)) - (this.tileSize * 2);
        this.height = (height - (this.padding * 2)) - (this.tileSize * 2);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        var leftX = xPos + padding;
        var rightX = leftX + width;
        var topY = yPos + padding;
        var bottomY = topY + height;

        GuiTextures.TILE_GUI_NW.blit(guiGraphics, leftX, topY);
        GuiTextures.TILE_GUI_N.blit(guiGraphics, leftX + tileSize, topY, width, tileSize);
        GuiTextures.TILE_GUI_NE.blit(guiGraphics, rightX + tileSize, topY);
        GuiTextures.TILE_GUI_E.blit(guiGraphics, rightX + tileSize, topY + tileSize, tileSize, height);
        GuiTextures.TILE_GUI_SE.blit(guiGraphics, rightX + tileSize, bottomY + tileSize);
        GuiTextures.TILE_GUI_S.blit(guiGraphics, leftX + tileSize, bottomY + tileSize, width, tileSize);
        GuiTextures.TILE_GUI_SW.blit(guiGraphics, leftX, bottomY + tileSize);
        GuiTextures.TILE_GUI_W.blit(guiGraphics, leftX, topY + tileSize, tileSize, height);
        GuiTextures.TILE_GUI_C.blit(guiGraphics, leftX + tileSize, topY + tileSize, width, height);
    }
}
