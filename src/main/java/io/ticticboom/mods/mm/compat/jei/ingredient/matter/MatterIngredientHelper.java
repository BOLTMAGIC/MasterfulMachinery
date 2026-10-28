package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatterIngredientHelper implements IIngredientHelper<MatterIngredient> {
    @Override
    public @NotNull IIngredientType<MatterIngredient> getIngredientType() {
        return MMJeiIngredients.REPLICATION_MATTER;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull MatterIngredient ingredient) {
        return MatterIngredientNames.displayName(ingredient.matter()).getString();
    }

    @Override
    public @NotNull String getUniqueId(@NotNull MatterIngredient ingredient, @NotNull UidContext context) {
        return "replication/matter/" + ingredient.matter();
    }

    @Override
    public @NotNull Identifier getResourceLocation(@NotNull MatterIngredient ingredient) {
        return ingredient.matter();
    }

    @Override
    public @NotNull MatterIngredient copyIngredient(@NotNull MatterIngredient ingredient) {
        return new MatterIngredient(ingredient.matter(), ingredient.amount(), ingredient.color());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable MatterIngredient ingredient) {
        return ingredient == null ? "Error" : ingredient.matter().toString();
    }
}
