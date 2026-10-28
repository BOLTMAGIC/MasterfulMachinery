package io.ticticboom.mods.mm.client.texture;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public record GuiTexture(Identifier texture, int u, int v, int uW, int vH, int texW, int texH) {
    public static GuiTexture of(Identifier texture, int u, int v, int uW, int vH, int texW, int texH) {
        return new GuiTexture(texture, u, v, uW, vH, texW, texH);
    }

    public void blit(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, uW, vH, uW, vH, texW, texH);
    }

    public void blit(GuiGraphicsExtractor guiGraphics, int x, int y, int sW, int sH) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, sW, sH, uW, vH, texW, texH);
    }
}
