package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class EntityIngredientType implements IIngredientType<EntityIngredient> {
    @Override
    public @NotNull Class<? extends EntityIngredient> getIngredientClass() {
        return EntityIngredient.class;
    }
}
