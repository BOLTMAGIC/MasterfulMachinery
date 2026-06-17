package io.ticticboom.mods.mm.compat.jei.ingredient.pncr;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class PneumaticAirIngredientType  implements IIngredientType<PneumaticAirStack> {
    @Override
    public @NotNull Class<? extends PneumaticAirStack> getIngredientClass() {
        return PneumaticAirStack.class;
    }
}
