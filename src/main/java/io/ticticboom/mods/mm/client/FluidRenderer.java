package io.ticticboom.mods.mm.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidRenderer {

    public static final FluidRenderer INSTANCE = new FluidRenderer(16);

    private final int width;

    public FluidRenderer(int width) {
        this.width = width;
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, final int xPosition, final int yPosition, FluidStack stack, final int height) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, FluidSprites.still(stack), xPosition, yPosition, width, height, FluidSprites.tint(stack));
    }
}
