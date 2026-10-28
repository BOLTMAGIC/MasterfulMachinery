package io.ticticboom.mods.mm.compat.jei.ingredient.emc;

import mezz.jei.api.ingredients.IIngredientType;

public class EmcIngredientType implements IIngredientType<EmcStack> {
    @Override
    public Class<? extends EmcStack> getIngredientClass() {
        return EmcStack.class;
    }
}
