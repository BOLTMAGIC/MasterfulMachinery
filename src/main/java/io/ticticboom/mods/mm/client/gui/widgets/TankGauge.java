package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.PortGuiLayout;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * One big tank for fluid and chemical ports, in the same frame as the energy port's bar.
 * All coordinates are absolute screen coordinates.
 */
public final class TankGauge {
    private static final int INNER_W = PortGuiLayout.TANK_W - 2;
    private static final int INNER_H = PortGuiLayout.TANK_H - 2;
    private static final int LABEL_MAX_W = INNER_W - 6;

    private TankGauge() {
    }

    public static void drawFrame(GuiGraphics gfx, int x, int y) {
        gfx.blit(Ref.UiTextures.SLOT_PARTS, x, y, 89, 78, PortGuiLayout.TANK_W, PortGuiLayout.TANK_H);
    }

    /**
     * Fills the tank from the bottom with the tinted sprite, tiled so the texture isn't stretched.
     */
    public static void drawFill(GuiGraphics gfx, int x, int y, double fraction, TextureAtlasSprite sprite, int tint) {
        int filled = (int) Math.round(INNER_H * Math.max(0, Math.min(1, fraction)));
        if (filled <= 0) {
            return;
        }
        int left = x + 1;
        int bottom = y + 1 + INNER_H;
        int top = bottom - filled;
        float alpha = ((tint >> 24) & 0xFF) / 255f;
        gfx.setColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, alpha == 0 ? 1 : alpha);
        gfx.enableScissor(left, top, left + INNER_W, bottom);
        for (int ty = bottom - 16; ty > top - 16; ty -= 16) {
            for (int tx = left; tx < left + INNER_W; tx += 16) {
                gfx.blit(tx, ty, 0, 16, 16, sprite);
            }
        }
        gfx.disableScissor();
        gfx.setColor(1, 1, 1, 1);
    }

    /**
     * Writes the contents on one line near the bottom of the tank, falling back to shorter forms
     * (no name, then compact numbers) when it doesn't fit.
     */
    public static void drawLabel(GuiGraphics gfx, Font font, int x, int y, @Nullable Component name, long amount, long capacity, String unit) {
        String grouped = CountFormat.grouped(amount) + " / " + CountFormat.grouped(capacity) + " " + unit;
        String compact = CountFormat.compact(amount) + " / " + CountFormat.compact(capacity) + " " + unit;
        Component[] candidates = name == null
                ? new Component[]{Component.literal(grouped), Component.literal(compact)}
                : new Component[]{name.copy().append("  " + grouped), Component.literal(grouped),
                name.copy().append("  " + compact), Component.literal(compact)};
        Component label = candidates[candidates.length - 1];
        for (Component candidate : candidates) {
            if (font.width(candidate) <= LABEL_MAX_W) {
                label = candidate;
                break;
            }
        }
        int textX = x + (PortGuiLayout.TANK_W - font.width(label)) / 2;
        gfx.drawString(font, label, textX, y + PortGuiLayout.TANK_H - 14, 0xFFFFFF, true);
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y) {
        return WidgetUtils.isPointerWithinSized(mouseX, mouseY, x, y, PortGuiLayout.TANK_W, PortGuiLayout.TANK_H);
    }
}
