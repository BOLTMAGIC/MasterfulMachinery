package io.ticticboom.mods.mm.compat.jei.ingredient.radiation;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class RadiationIngredientType implements IIngredientType<RadiationIngredient> {
    @Override
    public @NotNull Class<? extends RadiationIngredient> getIngredientClass() {
        return RadiationIngredient.class;
    }
}
