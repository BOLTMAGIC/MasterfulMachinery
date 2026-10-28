package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class MatterIngredientType implements IIngredientType<MatterIngredient> {
    @Override
    public @NotNull Class<? extends MatterIngredient> getIngredientClass() {
        return MatterIngredient.class;
    }
}
