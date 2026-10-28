package io.ticticboom.mods.mm.compat.jei.ingredient.energy;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.util.NumberText;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnergyIngredientRenderer implements IIngredientRenderer<EnergyStack> {

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, @NotNull EnergyStack ingredient) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, 0, 0, 19, 62, 16, 16, 256, 256);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EnergyStack ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        String amount = ingredient.max() > ingredient.min()
                ? NumberText.grouped(ingredient.min()) + " – " + NumberText.grouped(ingredient.max())
                : NumberText.grouped(ingredient.max());
        result.add(Component.translatable("jei.mm.ingredient.energy.amount", amount));
        return result;
    }
}
