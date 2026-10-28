package io.ticticboom.mods.mm.port;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import org.jetbrains.annotations.Nullable;

public interface IRecipeDemandListener {
    void setRecipeDemand(long gameTime, @Nullable RecipeModel recipe);
}
