package io.ticticboom.mods.mm.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * "&lt; All layers &gt;" selector drawn over the JEI structure view.
 * Layer -1 shows the whole structure, 0..layerCount-1 shows a single Y layer from the bottom.
 */
public class StructureLayerWidget implements IRecipeWidget, IJeiInputHandler {
    private static final int ARROW_WIDTH = 9;
    private static final int HEIGHT = 11;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_ARROW = 0xFFA0A0A0;
    private static final int COLOR_ARROW_HOVER = 0xFFFFFF55;
    private static final int COLOR_BACKDROP = 0x80000000;

    private final ScreenRectangle area;
    private final int layerCount;
    private final IntSupplier layer;
    private final IntConsumer setLayer;

    public StructureLayerWidget(int x, int y, int width, int layerCount, IntSupplier layer, IntConsumer setLayer) {
        this.area = new ScreenRectangle(x, y, width, HEIGHT);
        this.layerCount = layerCount;
        this.layer = layer;
        this.setLayer = setLayer;
    }

    @Override
    public ScreenPosition getPosition() {
        return area.position();
    }

    @Override
    public ScreenRectangle getArea() {
        return area;
    }

    @Override
    public void drawWidget(GuiGraphics gfx, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int width = area.width();
        gfx.fill(0, 0, width, HEIGHT, COLOR_BACKDROP);

        boolean hovered = mouseY >= 0 && mouseY < HEIGHT && mouseX >= 0 && mouseX < width;
        int leftColor = hovered && mouseX < ARROW_WIDTH ? COLOR_ARROW_HOVER : COLOR_ARROW;
        int rightColor = hovered && mouseX >= width - ARROW_WIDTH ? COLOR_ARROW_HOVER : COLOR_ARROW;
        gfx.drawString(font, "<", 2, 2, leftColor, false);
        gfx.drawString(font, ">", width - ARROW_WIDTH + 2, 2, rightColor, false);

        var label = font.plainSubstrByWidth(labelFor(layer.getAsInt()).getString(), width - 2 * ARROW_WIDTH);
        gfx.drawString(font, label, (width - font.width(label)) / 2, 2, COLOR_TEXT, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        // JEI asks every widget for a tooltip wherever the mouse is over the recipe, so check the bounds here
        if (mouseX >= 0 && mouseX < area.width() && mouseY >= 0 && mouseY < HEIGHT) {
            tooltip.add(Component.translatable("jei.mm.structure.layer.hint"));
        }
    }

    // JEI passes mouse coordinates relative to getArea()'s top-left corner
    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
        InputConstants.Key key = input.getKey();
        if (key.getType() != InputConstants.Type.MOUSE || key.getValue() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        double relX = mouseX;
        int step;
        if (relX < ARROW_WIDTH) {
            step = -1;
        } else if (relX >= area.width() - ARROW_WIDTH) {
            step = 1;
        } else {
            return false;
        }
        if (!input.isSimulate()) {
            step(step);
        }
        return true;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        // scrolling up moves up the structure
        step(scrollDelta > 0 ? 1 : -1);
        return true;
    }

    private void step(int direction) {
        // cycle through: all, 0, 1, ... layerCount-1, all
        int states = layerCount + 1;
        int current = layer.getAsInt() + 1;
        int next = Math.floorMod(current + direction, states);
        setLayer.accept(next - 1);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private Component labelFor(int layer) {
        if (layer < 0) {
            return Component.translatable("jei.mm.structure.layer.all");
        }
        return Component.translatable("jei.mm.structure.layer", layer + 1, layerCount);
    }
}
