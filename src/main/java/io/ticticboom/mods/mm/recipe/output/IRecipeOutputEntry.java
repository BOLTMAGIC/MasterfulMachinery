package io.ticticboom.mods.mm.recipe.output;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

public interface IRecipeOutputEntry {
    boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state);
    void output(Level level, RecipeStorages storages, RecipeStateModel state);
    default void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state);

    /**
     * What this run puts into the output ports when it finishes, as the state's rolls decide it (chance, weighted
     * pick). RecipeOutputs checks that the outputs of all entries fit together.
     */
    default java.util.List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        return java.util.List.of();
    }

    /** What this run may put into the output ports every tick ("per_tick"); the recipe waits while it doesn't fit. */
    default java.util.List<IPortIngredient> plannedTickOutputs(Level level, RecipeStateModel state) {
        return java.util.List.of();
    }

    default java.util.List<DisplayedOutput> displayedOutputs() {
        return java.util.List.of();
    }
    void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid);

    JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject jsonObject);
}
