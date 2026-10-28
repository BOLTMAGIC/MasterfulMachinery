package io.ticticboom.mods.mm.compat.jei.ingredient.heat;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class HeatIngredientType implements IIngredientType<HeatStack> {
    @Override
    public @NotNull Class<? extends HeatStack> getIngredientClass() {
        return HeatStack.class;
    }
}
