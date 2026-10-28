package io.ticticboom.mods.mm.port;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

public interface IPortIngredient {
    boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state);
    void process(Level level, RecipeStorages storages, RecipeStateModel state);
    default void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state);
    void output(Level level, RecipeStorages storages, RecipeStateModel state);
    default void outputTick(Level level, RecipeStorages storages, RecipeStateModel state) {}

    /**
     * Whether canOutput/output only touch output storages that {@link IPortStorage#simulationCopy} can copy, so
     * OutputSimulation can try this output next to the recipe's others.
     */
    default boolean canSimulateOutput() {
        return false;
    }

    /**
     * What this is in an AE2 pattern (item, fluid, chemical with its amount), or null when a pattern can't carry it
     * (energy, mana..., or a ranged amount, which no pattern holds): the Pattern Port then takes it from the
     * machine's ports.
     */
    @org.jetbrains.annotations.Nullable
    default io.ticticboom.mods.mm.pattern.PatternIngredient asPatternIngredient() {
        return null;
    }
    void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot);
    default void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    default net.minecraft.network.chat.Component displayName() {
        return null;
    }

    default AmountRange getAmountRange() {
        return null;
    }

    /**
     * A copy of this ingredient with its amount multiplied by {@code factor}, for outputs with "scale_by" and the
     * flex recipe cost. A ranged amount keeps its roll.
     *
     * @return null if this ingredient can't be scaled
     */
    @org.jetbrains.annotations.Nullable
    default IPortIngredient scaled(double factor) {
        return null;
    }

    @org.jetbrains.annotations.Nullable
    default PortContent display() {
        return null;
    }

    JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json);
    JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json);

}
