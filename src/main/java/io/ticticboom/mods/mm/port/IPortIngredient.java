package io.ticticboom.mods.mm.port;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public interface IPortIngredient {
    boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state);
    void process(Level level, RecipeStorages storages, RecipeStateModel state);
    default void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state);
    void output(Level level, RecipeStorages storages, RecipeStateModel state);
    default void outputTick(Level level, RecipeStorages storages, RecipeStateModel state) {}
    void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot);
    default void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json);
    JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json);

    /**
     * @return an item to show for this ingredient outside JEI (e.g. the controller screen), or empty
     */
    default ItemStack displayItem() {
        return ItemStack.EMPTY;
    }

    /**
     * @return a fluid to show for this ingredient outside JEI (e.g. the controller screen), or empty
     */
    default FluidStack displayFluid() {
        return FluidStack.EMPTY;
    }

    /**
     * @return how to show this ingredient outside JEI (e.g. the controller screen) with its amount,
     * or null if it has no display
     */
    @Nullable
    default PortContent display() {
        ItemStack item = displayItem();
        if (!item.isEmpty()) {
            return PortContent.item(item, item.getCount());
        }
        FluidStack fluid = displayFluid();
        if (!fluid.isEmpty()) {
            return PortContent.fluid(fluid, 0);
        }
        return null;
    }
}
