package io.ticticboom.mods.mm.recipe.output;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IRecipeOutputEntry {
    boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state);
    void output(Level level, RecipeStorages storages, RecipeStateModel state);
    default void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state);
    void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid);

    /**
     * @return what this entry can output, each with its chance (0-1), for the controller screen
     */
    default List<DisplayedOutput> displayedOutputs() {
        return List.of();
    }

    JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject jsonObject);
}
