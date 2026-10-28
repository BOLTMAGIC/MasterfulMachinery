package io.ticticboom.mods.mm.compat.jei.ingredient.radiation;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.util.RadiationText;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RadiationIngredientHelper implements IIngredientHelper<RadiationIngredient> {
    @Override
    public @NotNull IIngredientType<RadiationIngredient> getIngredientType() {
        return MMJeiIngredients.NUCLEAR_RADIATION;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull RadiationIngredient ingredient) {
        return RadiationText.isotopeName(ingredient.isotope()).getString();
    }

    @Override
    public @NotNull Object getUid(@NotNull RadiationIngredient ingredient, @NotNull UidContext context) {
        return "nuclear_radiation/" + (ingredient.isotope() == null ? "any" : ingredient.isotope());
    }

    @Override
    public @NotNull Identifier getIdentifier(@NotNull RadiationIngredient ingredient) {
        var parsed = ingredient.isotope() == null ? null : Identifier.tryParse(ingredient.isotope());
        return parsed != null ? parsed : Ref.id("nuclear_radiation/any");
    }

    @Override
    public @NotNull RadiationIngredient copyIngredient(@NotNull RadiationIngredient ingredient) {
        return new RadiationIngredient(ingredient.isotope(), ingredient.min(), ingredient.max());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable RadiationIngredient ingredient) {
        return ingredient == null ? "Error" : String.valueOf(ingredient.isotope());
    }
}
