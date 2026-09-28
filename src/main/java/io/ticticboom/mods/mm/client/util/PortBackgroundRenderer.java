package io.ticticboom.mods.mm.client.util;

import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the port's flat panel with fixed-width borders and at most nine textured quads. */
public final class PortBackgroundRenderer {
    private static final int TEXTURE_SIZE = 12;
    private static final int BORDER = 4;

    private PortBackgroundRenderer() {}

    public static void draw(GuiGraphics gfx, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int bx = Math.min(BORDER, width / 2);
        int by = Math.min(BORDER, height / 2);

        // Forge's blitNineSlicedSized repeats the 4x4 centre with an individual GPU submission
        // per tile. An 8-row, 12-column port needs 3,591 background blits per frame that way.
        // Stretch each patch instead; window size must not multiply the number of draw calls.
        for (int row = 0; row < 3; row++) {
            int dy = row == 0 ? y : row == 1 ? y + by : y + height - by;
            int dh = row == 1 ? height - 2 * by : by;
            int v = row == 0 ? 0 : row == 1 ? by : TEXTURE_SIZE - by;
            int sh = row == 1 ? TEXTURE_SIZE - 2 * by : by;
            for (int column = 0; column < 3; column++) {
                int dx = column == 0 ? x : column == 1 ? x + bx : x + width - bx;
                int dw = column == 1 ? width - 2 * bx : bx;
                int u = column == 0 ? 0 : column == 1 ? bx : TEXTURE_SIZE - bx;
                int sw = column == 1 ? TEXTURE_SIZE - 2 * bx : bx;
                if (dw > 0 && dh > 0) {
                    gfx.blit(Ref.UiTextures.TILING_GUI, dx, dy, dw, dh,
                            (float) u, (float) v, sw, sh, TEXTURE_SIZE, TEXTURE_SIZE);
                }
            }
        }
    }
}
