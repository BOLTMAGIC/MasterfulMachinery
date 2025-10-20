package io.ticticboom.mods.mm.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.extensions.IForgeFont;

public class TextRenderUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static FormattedText ellipsizeAlways(Font font, FormattedText text, int maxWidth) {
        final int strWidth = font.width(text);
        final int ellipsisWidth = font.width(IForgeFont.ELLIPSIS);
        if (strWidth > maxWidth) {
            return FormattedText.composite(
                    font.substrByWidth(text, maxWidth - ellipsisWidth),
                    IForgeFont.ELLIPSIS);
        } else if (strWidth + ellipsisWidth > maxWidth) {
            return FormattedText.composite(
                    font.substrByWidth(text, maxWidth - ellipsisWidth),
                    IForgeFont.ELLIPSIS);
        }
        return FormattedText.composite(text, IForgeFont.ELLIPSIS);
    }

    public static void renderWordWrapLimit(GuiGraphics guiGraphics, Font font, FormattedText text, int x, int y, int lineWidth, int lineLimit, int color) {
        var lineHeight = font.lineHeight;
        var split = font.getSplitter().splitLines(text, lineWidth, Style.EMPTY);
        for (int i = 0; i < split.size(); i++) {
            var isLimit = i == lineLimit - 1;
            var isEnd = i == split.size() - 1;
            var lineText = split.get(i);

            if (isLimit && !isEnd) {
                lineText = ellipsizeAlways(font, lineText, lineWidth);
            }

            guiGraphics.drawString(font, lineText.getString(), x, y, color, false);
            y += lineHeight;
            if (isLimit) {
                break;
            }
        }
    }

    public static void renderWordWrapLimit(GuiGraphics guiGraphics, FormattedText text, int x, int y, int lineWidth, int lineLimit, int color) {
        renderWordWrapLimit(guiGraphics, mc.font, text, x, y, lineWidth, lineLimit, color);
    }

    public static void renderWordWrapLimit(GuiGraphics guiGraphics, String text, int x, int y, int lineWidth, int lineLimit, int color) {
        var fText = FormattedText.of(text);
        renderWordWrapLimit(guiGraphics, fText, x, y, lineWidth, lineLimit, color);
    }
}
