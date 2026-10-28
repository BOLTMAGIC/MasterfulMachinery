package io.ticticboom.mods.mm.client.util;

import io.ticticboom.mods.mm.client.util.TextColors;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.extensions.IFontExtension;

public class TextRenderUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void drawClipped(GuiGraphicsExtractor gfx, Font font, Component text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) {
            return;
        }
        FormattedText clipped = font.ellipsize(text, maxWidth);
        gfx.text(font, Language.getInstance().getVisualOrder(clipped), x, y, TextColors.argb(color), false);
    }

    public static FormattedText ellipsizeAlways(Font font, FormattedText text, int maxWidth) {
        final int strWidth = font.width(text);
        final int ellipsisWidth = font.width(IFontExtension.ELLIPSIS);
        if (strWidth > maxWidth) {
            return FormattedText.composite(
                    font.substrByWidth(text, maxWidth - ellipsisWidth),
                    IFontExtension.ELLIPSIS);
        } else if (strWidth + ellipsisWidth > maxWidth) {
            return FormattedText.composite(
                    font.substrByWidth(text, maxWidth - ellipsisWidth),
                    IFontExtension.ELLIPSIS);
        }
        return FormattedText.composite(text, IFontExtension.ELLIPSIS);
    }

    public static void renderWordWrapLimit(GuiGraphicsExtractor guiGraphics, Font font, FormattedText text, int x, int y, int lineWidth, int lineLimit, int color) {
        var lineHeight = font.lineHeight;
        var split = font.getSplitter().splitLines(text, lineWidth, Style.EMPTY);
        for (int i = 0; i < split.size(); i++) {
            var isLimit = i == lineLimit - 1;
            var isEnd = i == split.size() - 1;
            var lineText = split.get(i);

            if (isLimit && !isEnd) {
                lineText = ellipsizeAlways(font, lineText, lineWidth);
            }

            guiGraphics.text(font, lineText.getString(), x, y, TextColors.argb(color), false);
            y += lineHeight;
            if (isLimit) {
                break;
            }
        }
    }

    public static void renderWordWrapLimit(GuiGraphicsExtractor guiGraphics, FormattedText text, int x, int y, int lineWidth, int lineLimit, int color) {
        renderWordWrapLimit(guiGraphics, mc.font, text, x, y, lineWidth, lineLimit, color);
    }

    public static void renderWordWrapLimit(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int lineWidth, int lineLimit, int color) {
        var fText = FormattedText.of(text);
        renderWordWrapLimit(guiGraphics, fText, x, y, lineWidth, lineLimit, color);
    }
}
