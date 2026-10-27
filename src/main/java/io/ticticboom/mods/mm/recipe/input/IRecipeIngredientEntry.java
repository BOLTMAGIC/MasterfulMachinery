package io.ticticboom.mods.mm.recipe.input;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

public interface IRecipeIngredientEntry {
    boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state);
    void process(Level level, RecipeStorages storages, RecipeStateModel state);
    default void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state);
    void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid);
    /** How many JEI slots setRecipe adds without a focus, for sizing the recipe's slot grid. */
    default int jeiSlotCount(RecipeModel model) {
        return 1;
    }
    JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json);
}
