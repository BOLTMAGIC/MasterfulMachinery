package io.ticticboom.mods.mm.compat.jei.ingredient.create;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class CreateRotationIngredientType implements IIngredientType<CreateRotationStack> {
    @Override
    public @NotNull Class<? extends CreateRotationStack> getIngredientClass() {
        return CreateRotationStack.class;
    }
}
