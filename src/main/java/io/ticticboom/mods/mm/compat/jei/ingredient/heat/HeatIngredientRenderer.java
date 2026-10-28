package io.ticticboom.mods.mm.compat.jei.ingredient.heat;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HeatIngredientRenderer implements IIngredientRenderer<HeatStack> {

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, @NotNull HeatStack ingredient) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, 0, 0, 0, 79, 16, 16, 256, 256);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(HeatStack ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("jei.mm.ingredient.mekanism_heat.title"));
        result.add(Component.translatable("jei.mm.ingredient.mekanism_heat.amount", ingredient.amount()));
        return result;
    }
}
