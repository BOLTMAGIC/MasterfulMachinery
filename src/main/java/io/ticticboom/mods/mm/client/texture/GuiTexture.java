package io.ticticboom.mods.mm.client.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public record GuiTexture(ResourceLocation texture, int u, int v, int uW, int vH, int texW, int texH) {
    public static GuiTexture of(ResourceLocation texture, int u, int v, int uW, int vH, int texW, int texH) {
        return new GuiTexture(texture, u, v, uW, vH, texW, texH);
    }

    public void blit(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(texture, x, y, uW, vH, u, v, uW, vH, texW, texH);
    }

    public void blit(GuiGraphics guiGraphics, int x, int y, int sW, int sH) {
        guiGraphics.blit(texture, x, y, sW, sH, u, v, uW, vH, texW, texH);
    }
}
