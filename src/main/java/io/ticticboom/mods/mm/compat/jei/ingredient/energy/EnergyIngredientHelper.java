package io.ticticboom.mods.mm.compat.jei.ingredient.energy;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyIngredientHelper implements IIngredientHelper<EnergyStack> {
    @Override
    public @NotNull IIngredientType<EnergyStack> getIngredientType() {
        return MMJeiIngredients.ENERGY;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull EnergyStack ingredient) {
        return "Forge Energy";
    }

    @Override
    public @NotNull String getUniqueId(@NotNull EnergyStack ingredient, @NotNull UidContext context) {
        return "energy";
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull EnergyStack ingredient) {
        return Ref.id("energy");
    }

    @Override
    public @NotNull EnergyStack copyIngredient(EnergyStack ingredient) {
        return new EnergyStack(ingredient.amount());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable EnergyStack ingredient) {
        return "Error";
    }
}
