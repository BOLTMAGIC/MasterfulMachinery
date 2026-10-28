package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntityIngredientRenderer implements IIngredientRenderer<EntityIngredient> {

    @Override
    public void render(GuiGraphicsExtractor gfx, @NotNull EntityIngredient ingredient) {
        var icon = EntityIngredientNames.icon(ingredient);
        if (!icon.isEmpty()) {
            gfx.item(icon, 0, 0);
            return;
        }
        gfx.fill(0, 0, 16, 16, 0xFF1B1B1B);
        gfx.fill(1, 1, 15, 15, 0xFF5E35B0);
        gfx.fill(4, 5, 7, 8, 0xFFEFE3FF);
        gfx.fill(9, 5, 12, 8, 0xFFEFE3FF);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EntityIngredient ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("jei.mm.ingredient.entity.title", EntityIngredientNames.displayName(ingredient)));
        result.add(Component.translatable("jei.mm.ingredient.entity.amount", ingredient.amount()));
        return result;
    }
}
