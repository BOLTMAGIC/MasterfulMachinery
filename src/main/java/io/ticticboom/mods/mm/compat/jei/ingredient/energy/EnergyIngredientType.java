package io.ticticboom.mods.mm.compat.jei.ingredient.energy;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class EnergyIngredientType implements IIngredientType<EnergyStack> {
    @Override
    public @NotNull Class<? extends EnergyStack> getIngredientClass() {
        return EnergyStack.class;
    }
}
