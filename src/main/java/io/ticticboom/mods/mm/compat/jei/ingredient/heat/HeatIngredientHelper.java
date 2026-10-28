package io.ticticboom.mods.mm.compat.jei.ingredient.heat;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeatIngredientHelper implements IIngredientHelper<HeatStack> {
    @Override
    public @NotNull IIngredientType<HeatStack> getIngredientType() {
        return MMJeiIngredients.MEKANISM_HEAT;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull HeatStack ingredient) {
        return "Mekanism Heat";
    }

    @Override
    public @NotNull Object getUid(@NotNull HeatStack ingredient, @NotNull UidContext context) {
        return "mekanism/heat";
    }

    @Override
    public @NotNull Identifier getIdentifier(@NotNull HeatStack ingredient) {
        return Ref.id("mekanism/heat");
    }

    @Override
    public @NotNull HeatStack copyIngredient(@NotNull HeatStack ingredient) {
        return new HeatStack(ingredient.amount());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable HeatStack ingredient) {
        return "Error";
    }
}
