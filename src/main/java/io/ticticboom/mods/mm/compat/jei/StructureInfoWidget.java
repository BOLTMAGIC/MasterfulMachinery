package io.ticticboom.mods.mm.compat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;

/**
 * A small blue "i" in a corner of the JEI structure view; hovering it explains the mouse controls.
 */
public class StructureInfoWidget implements IRecipeWidget {
    public static final int SIZE = 10;
    private static final int COLOR_BORDER = 0xFF1E3F8A;
    private static final int COLOR_FILL = 0xFF3B6FD6;
    private static final int COLOR_FILL_HOVER = 0xFF5B8FF6;

    private final ScreenPosition position;

    public StructureInfoWidget(int x, int y) {
        this.position = new ScreenPosition(x, y);
    }

    @Override
    public ScreenPosition getPosition() {
        return position;
    }

    private static boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX < SIZE && mouseY >= 0 && mouseY < SIZE;
    }

    @Override
    public void drawWidget(GuiGraphics gfx, double mouseX, double mouseY) {
        gfx.fill(0, 0, SIZE, SIZE, COLOR_BORDER);
        gfx.fill(1, 1, SIZE - 1, SIZE - 1, isHovered(mouseX, mouseY) ? COLOR_FILL_HOVER : COLOR_FILL);
        var font = Minecraft.getInstance().font;
        gfx.drawString(font, "i", (SIZE - font.width("i")) / 2 + 1, 1, 0xFFFFFFFF, false);
    }

    // JEI asks every widget for a tooltip wherever the mouse is over the recipe, so check the bounds here
    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (!isHovered(mouseX, mouseY)) {
            return;
        }
        tooltip.add(Component.translatable("jei.mm.structure.controls").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("jei.mm.structure.controls.rotate").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.mm.structure.controls.zoom").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.mm.structure.controls.pan").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.mm.structure.controls.layers").withStyle(ChatFormatting.GRAY));
    }
}
