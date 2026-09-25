package io.ticticboom.mods.mm.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Compact counts (1K / 1.5M / 2B) and grouped amounts (234 000) shared by JEI and the port screens.
 */
public final class CountFormat {
    private static final DecimalFormat GROUPED;

    static {
        var symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        GROUPED = new DecimalFormat("#,##0", symbols);
    }

    private CountFormat() {
    }

    /** Formats a count into a compact AE2-style string (1K / 1M / 1B). */
    public static String compact(long count) {
        if (count >= 1_000_000_000) {
            long tenthsOfB = Math.round(count / 100_000_000.0);
            long whole = tenthsOfB / 10;
            long tenths = tenthsOfB % 10;
            return tenths == 0 ? whole + "B" : whole + "." + tenths + "B";
        } else if (count >= 1_000_000) {
            long tenthsOfM = Math.round(count / 100_000.0);
            // Rounding can push 999.95M → 1B
            if (tenthsOfM >= 10_000) {
                return (tenthsOfM / 10_000) + "B";
            }
            long whole = tenthsOfM / 10;
            long tenths = tenthsOfM % 10;
            return tenths == 0 ? whole + "M" : whole + "." + tenths + "M";
        } else if (count >= 1_000) {
            long tenthsOfK = Math.round(count / 100.0);
            // Rounding can push 999.95K → 1M
            if (tenthsOfK >= 10_000) {
                return (tenthsOfK / 10_000) + "M";
            }
            long whole = tenthsOfK / 10;
            long tenths = tenthsOfK % 10;
            return tenths == 0 ? whole + "K" : whole + "." + tenths + "K";
        }
        return String.valueOf(count);
    }

    /** Formats an amount with space-separated thousands (234 000). */
    public static String grouped(long amount) {
        synchronized (GROUPED) {
            return GROUPED.format(amount);
        }
    }

    /** Renders an AE2-style count label at the bottom-right of an 18x18 slot whose corner is at slotX/slotY. */
    public static void drawSlotCount(GuiGraphics gfx, int slotX, int slotY, long count) {
        String text = compact(count);
        var font = Minecraft.getInstance().font;
        // Use half-size font when the string is longer than 3 chars to fit inside the slot
        float scale = text.length() > 3 ? 0.5f : 1.0f;
        var pose = gfx.pose();
        pose.pushPose();
        // Anchor to the bottom-right corner of the slot inner area; z=200 draws above the item texture
        pose.translate(slotX + 17, slotY + 17, 200);
        pose.scale(scale, scale, 1.0f);
        gfx.drawString(font, text, -font.width(text), -font.lineHeight, 0xFFFFFF, true);
        pose.popPose();
    }
}
