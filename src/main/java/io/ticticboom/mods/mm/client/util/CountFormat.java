package io.ticticboom.mods.mm.client.util;

import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.util.NumberText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CountFormat {

    private CountFormat() {
    }

    public static String compact(long count) {
        if (count >= 1_000_000_000) {
            return withSuffix(Math.round(count / 100_000_000.0), "B", null);
        }
        if (count >= 1_000_000) {
            return withSuffix(Math.round(count / 100_000.0), "M", "B");
        }
        if (count >= 1_000) {
            return withSuffix(Math.round(count / 100.0), "K", "M");
        }
        return String.valueOf(count);
    }

    private static String withSuffix(long tenths, String suffix, String next) {
        if (next != null && tenths >= 10_000) {
            return (tenths / 10_000) + next;
        }
        long whole = tenths / 10;
        long fraction = tenths % 10;
        return fraction == 0 ? whole + suffix : whole + "." + fraction + suffix;
    }

    public static String grouped(long amount) {
        return NumberText.grouped(amount);
    }

    public static void drawSlotCount(GuiGraphicsExtractor gfx, int slotX, int slotY, long count) {
        // z=200 draws above the item texture
        drawSlotCount(gfx, slotX, slotY, count, 200);
    }

    /**
     * @param z depth to add; a JEI slot overlay is already drawn at z=200, so it passes 0 (another 200 would reach the
     *          tooltip's z=400 and show through tooltips drawn over the slot)
     */
    public static void drawSlotCount(GuiGraphicsExtractor gfx, int slotX, int slotY, long count, int z) {
        String text = compact(count);
        var font = Minecraft.getInstance().font;
        float scale = text.length() > 3 ? 0.5f : 1.0f;
        var pose = gfx.pose();
        pose.pushMatrix();
        // Anchor to the bottom-right corner of the slot inner area
        pose.translate(slotX + 17, slotY + 17);
        pose.scale(scale, scale);
        gfx.text(font, text, -font.width(text), -font.lineHeight, TextColors.argb(0xFFFFFF), true);
        pose.popMatrix();
    }
}
