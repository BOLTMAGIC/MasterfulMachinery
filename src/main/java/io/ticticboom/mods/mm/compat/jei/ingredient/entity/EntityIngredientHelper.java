package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityIngredientHelper implements IIngredientHelper<EntityIngredient> {
    @Override
    public @NotNull IIngredientType<EntityIngredient> getIngredientType() {
        return MMJeiIngredients.ENTITY;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull EntityIngredient ingredient) {
        return EntityIngredientNames.displayName(ingredient).getString();
    }

    @Override
    public @NotNull Object getUid(@NotNull EntityIngredient ingredient, @NotNull UidContext context) {
        return "entity/" + (ingredient.tag() ? "tag/" : "") + ingredient.id();
    }

    @Override
    public @NotNull Identifier getIdentifier(@NotNull EntityIngredient ingredient) {
        return ingredient.id();
    }

    @Override
    public @NotNull EntityIngredient copyIngredient(@NotNull EntityIngredient ingredient) {
        return new EntityIngredient(ingredient.id(), ingredient.tag(), ingredient.amount());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable EntityIngredient ingredient) {
        return ingredient == null || ingredient.id() == null ? "Error" : ingredient.id().toString();
    }
}
