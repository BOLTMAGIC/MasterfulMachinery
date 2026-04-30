package io.ticticboom.mods.mm.compat.jei.ingredient.energy;

import io.ticticboom.mods.mm.Ref;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnergyIngredientRenderer implements IIngredientRenderer<EnergyStack> {

    @Override
    public void render(GuiGraphics gfx, @NotNull EnergyStack ingredient) {
        gfx.blit(Ref.UiTextures.SLOT_PARTS, 0, 0, 19, 62, 16, 16);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EnergyStack ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.literal(ingredient.amount() + " FE"));
        return result;
    }
}
