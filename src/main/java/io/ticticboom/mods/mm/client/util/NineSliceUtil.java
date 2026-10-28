package io.ticticboom.mods.mm.client.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * 1.21 removed GuiGraphics.blitNineSlicedSized. Its replacement, blitSprite, only nine-slices
 * textures registered as GUI sprites with an accompanying .mcmeta, and MM's panel texture lives
 * under textures/gui/parts rather than the sprite atlas. Rather than move the texture and change
 * how the panel looks, this reproduces the original nine-slice with plain stretched blits:
 * fixed-size corners, edges stretched along one axis, and a stretched centre.
 */
public final class NineSliceUtil {

    private NineSliceUtil() {
    }

    public static void blitNineSliced(GuiGraphics gfx, Identifier texture,
                                      int x, int y, int width, int height,
                                      int sliceLeft, int sliceTop, int sliceRight, int sliceBottom,
                                      int uWidth, int vHeight, int uOffset, int vOffset,
                                      int textureWidth, int textureHeight) {
        int innerW = Math.max(0, width - sliceLeft - sliceRight);
        int innerH = Math.max(0, height - sliceTop - sliceBottom);
        int srcInnerW = Math.max(1, uWidth - sliceLeft - sliceRight);
        int srcInnerH = Math.max(1, vHeight - sliceTop - sliceBottom);

        int rightU = uOffset + uWidth - sliceRight;
        int bottomV = vOffset + vHeight - sliceBottom;
        int rightX = x + width - sliceRight;
        int bottomY = y + height - sliceBottom;

        // corners, drawn at their natural size
        blit(gfx, texture, x, y, sliceLeft, sliceTop, uOffset, vOffset, sliceLeft, sliceTop, textureWidth, textureHeight);
        blit(gfx, texture, rightX, y, sliceRight, sliceTop, rightU, vOffset, sliceRight, sliceTop, textureWidth, textureHeight);
        blit(gfx, texture, x, bottomY, sliceLeft, sliceBottom, uOffset, bottomV, sliceLeft, sliceBottom, textureWidth, textureHeight);
        blit(gfx, texture, rightX, bottomY, sliceRight, sliceBottom, rightU, bottomV, sliceRight, sliceBottom, textureWidth, textureHeight);

        // edges, stretched along the spanning axis
        if (innerW > 0) {
            blit(gfx, texture, x + sliceLeft, y, innerW, sliceTop, uOffset + sliceLeft, vOffset, srcInnerW, sliceTop, textureWidth, textureHeight);
            blit(gfx, texture, x + sliceLeft, bottomY, innerW, sliceBottom, uOffset + sliceLeft, bottomV, srcInnerW, sliceBottom, textureWidth, textureHeight);
        }
        if (innerH > 0) {
            blit(gfx, texture, x, y + sliceTop, sliceLeft, innerH, uOffset, vOffset + sliceTop, sliceLeft, srcInnerH, textureWidth, textureHeight);
            blit(gfx, texture, rightX, y + sliceTop, sliceRight, innerH, rightU, vOffset + sliceTop, sliceRight, srcInnerH, textureWidth, textureHeight);
        }
        if (innerW > 0 && innerH > 0) {
            blit(gfx, texture, x + sliceLeft, y + sliceTop, innerW, innerH,
                    uOffset + sliceLeft, vOffset + sliceTop, srcInnerW, srcInnerH, textureWidth, textureHeight);
        }
    }

    private static void blit(GuiGraphics gfx, Identifier texture, int x, int y, int w, int h,
                             int u, int v, int uw, int vh, int texW, int texH) {
        if (w <= 0 || h <= 0) {
            return;
        }
        gfx.blit(texture, x, y, w, h, (float) u, (float) v, uw, vh, texW, texH);
    }
}
