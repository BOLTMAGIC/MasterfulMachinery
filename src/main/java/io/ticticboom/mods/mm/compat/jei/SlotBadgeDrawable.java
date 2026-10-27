package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.client.util.CountFormat;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public record SlotBadgeDrawable(int count, boolean notUsed) implements IDrawable {
    @Override
    public int getWidth() {
        return 18;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    @Override
    public void draw(GuiGraphics gfx, int x, int y) {
        if (notUsed) {
            gfx.drawString(Minecraft.getInstance().font, "x", x + 12, y + 12, 0xFF5555, true);
        }
        if (count > 1) {
            // JEI already draws overlays at z=200, above the item
            CountFormat.drawSlotCount(gfx, x, y, count, 0);
        }
    }
}
