package io.ticticboom.mods.mm.compat.jei.ingredient.pncr;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PneumaticAirIngredientHelper  implements IIngredientHelper<PneumaticAirStack>{
    @Override
    public @NotNull IIngredientType<PneumaticAirStack> getIngredientType() {
        return MMJeiIngredients.PNEUMATIC_AIR;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull PneumaticAirStack ingredient) {
        return "Pneumatic Air";
    }

    @Override
    public @NotNull String getUniqueId(@NotNull PneumaticAirStack ingredient, @NotNull UidContext context) {
        return "pneumaticcraft/air";
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull PneumaticAirStack ingredient) {
        return Ref.id("pneumaticcraft/air");
    }

    @Override
    public @NotNull PneumaticAirStack copyIngredient(@NotNull PneumaticAirStack ingredient) {
        return new PneumaticAirStack(ingredient.air(), ingredient.pressure());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable PneumaticAirStack ingredient) {
        return "Error";
    }
}
