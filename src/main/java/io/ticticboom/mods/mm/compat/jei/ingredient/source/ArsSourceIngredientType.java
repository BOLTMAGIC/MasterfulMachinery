package io.ticticboom.mods.mm.compat.jei.ingredient.source;

import mezz.jei.api.ingredients.IIngredientType;

public class ArsSourceIngredientType implements IIngredientType<ArsSourceStack> {
    @Override
    public Class<? extends ArsSourceStack> getIngredientClass() {
        return ArsSourceStack.class;
    }
}
