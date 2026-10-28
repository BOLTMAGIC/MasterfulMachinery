package io.ticticboom.mods.mm.client.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TankGauge {
    public static final int X = 7;
    public static final int Y = 24;
    public static final int W = 162;
    public static final int H = 80;
    private static final int INNER_W = W - 2;
    private static final int INNER_H = H - 2;
    private static final int LABEL_MAX_W = INNER_W - 6;

    private TankGauge() {
    }

    public static void drawFrame(GuiGraphicsExtractor gfx, int x, int y) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, x, y, 89, 78, W, H, 256, 256);
    }

    public static void drawFill(GuiGraphicsExtractor gfx, int x, int y, double fraction, TextureAtlasSprite sprite, int tint) {
        int filled = (int) Math.round(INNER_H * Math.max(0, Math.min(1, fraction)));
        if (filled <= 0) {
            return;
        }
        int left = x + 1;
        int bottom = y + 1 + INNER_H;
        int top = bottom - filled;
        int color = TextColors.argb(tint);
        gfx.enableScissor(left, top, left + INNER_W, bottom);
        for (int ty = bottom - 16; ty > top - 16; ty -= 16) {
            for (int tx = left; tx < left + INNER_W; tx += 16) {
                gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, color);
            }
        }
        gfx.disableScissor();
    }

    public static void drawEnergyFill(GuiGraphicsExtractor gfx, int x, int y, double fraction) {
        int filled = (int) Math.round(INNER_H * Math.max(0, Math.min(1, fraction)));
        if (filled <= 0) {
            return;
        }
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, x + 1, y + 1 + INNER_H - filled, 90, INNER_H - filled, INNER_W, filled, 256, 256);
    }

    public static void drawLabel(GuiGraphicsExtractor gfx, Font font, int x, int y, @Nullable Component name, long amount, long capacity, String unit) {
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
        int textX = x + (W - font.width(label)) / 2;
        gfx.text(font, label, textX, y + H - 14, TextColors.argb(0xFFFFFF), true);
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y) {
        return WidgetUtils.isPointerWithinSized(mouseX, mouseY, x, y, W, H);
    }
}
