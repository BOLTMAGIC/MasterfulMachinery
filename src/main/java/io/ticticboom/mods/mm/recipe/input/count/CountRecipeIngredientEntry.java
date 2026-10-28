package io.ticticboom.mods.mm.recipe.input.count;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * "mm:input/count": counts the matching items in the input ports without consuming them, and stores the number
 * under its "as" name for outputs with "scale_by". The ingredient's count is the least needed to start, "max" caps
 * the number. The count is taken when the recipe starts.
 */
public class CountRecipeIngredientEntry implements IRecipeIngredientEntry {

    @Getter
    private final String name;
    @Getter
    private final BaseItemPortIngredient ingredient;
    private final int max;

    public CountRecipeIngredientEntry(String name, BaseItemPortIngredient ingredient, int max) {
        this.name = name;
        this.ingredient = ingredient;
        this.max = max;
    }

    private int countIn(RecipeStorages storages) {
        long total = 0;
        for (ItemPortStorage storage : storages.getInputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (ingredient.matches(handler.getStackInSlot(slot))) {
                    total += handler.getActualCount(slot);
                    if (total >= max) {
                        return max;
                    }
                }
            }
        }
        return (int) total;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        int counted = countIn(storages);
        state.setCount(name, counted);
        return counted >= ingredient.getCount();
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        state.setCount(name, countIn(storages));
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, slot.getInnerX(), slot.getInnerY());
        int min = ingredient.getCount();
        slot.setBadgeCount(min);
        if (min > 1) {
            rSlot.setOverlay(new SlotBadgeDrawable(min, false), 0, 0);
        }
        rSlot.addItemStacks(ingredient.exampleStacks().stream().map(ItemStack::copy).toList());
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.count_input", name).withStyle(ChatFormatting.DARK_AQUA));
            if (max < Integer.MAX_VALUE) {
                list.add(Component.translatable("jei.mm.recipe.count_input_max", max).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("as", name);
        json.addProperty("min", ingredient.getCount());
        json.addProperty("max", max);
        json.addProperty("counted", storages == null ? 0 : countIn(storages));
        return json;
    }
}
