package io.ticticboom.mods.mm.port;

import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;

public interface ITickSpreadIngredient extends IPortIngredient {

    long resolveAmount(RecipeStateModel state);

    long extractFromInputs(RecipeStorages storages, long amount, boolean simulate);
}
