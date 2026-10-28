package io.ticticboom.mods.mm.compat.jei.ingredient.radiation;

import io.ticticboom.mods.mm.util.RadiationText;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadiationIngredientRenderer implements IIngredientRenderer<RadiationIngredient> {

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, @NotNull RadiationIngredient ingredient) {
        gfx.fill(0, 0, 16, 16, 0xFF1B1B1B);
        gfx.fill(1, 1, 15, 15, 0xFFE0C21A);
        gfx.fill(6, 6, 10, 10, 0xFF1B1B1B);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(RadiationIngredient ingredient, @NotNull TooltipFlag tooltipFlag) {
        String amount = ingredient.max() > ingredient.min()
                ? RadiationText.bq(ingredient.min()) + " – " + RadiationText.bq(ingredient.max())
                : RadiationText.bq(ingredient.max());
        return List.of(
                RadiationText.isotopeName(ingredient.isotope()).copy().withStyle(ChatFormatting.YELLOW),
                Component.translatable("jei.mm.ingredient.nuclear_radiation.amount", amount));
    }
}
