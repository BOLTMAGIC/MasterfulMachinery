package io.ticticboom.mods.mm.recipe.input.consume;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ConsumeRecipeIngredientEntry implements IRecipeIngredientEntry {

    // expose ingredient for lightweight capability checks
    @Getter
    private final IPortIngredient ingredient;
    private final double chance;
    @Getter
    private final boolean perTick;

    private boolean shouldRun = true;

    public ConsumeRecipeIngredientEntry(IPortIngredient ingredient, double chance, boolean perTick) {
        this.ingredient = ingredient;
        this.chance = chance;
        this.perTick = perTick;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        shouldRun = ChanceUtils.shouldProceed(chance);
        return ingredient.canProcess(level, storages, state);
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun) {
            ingredient.process(level, storages, state);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        // Call the ingredient's per-tick hook. Ingredients that are consumed per-tick
        // should implement their own processTick() to perform the actual extraction.
        // This prevents calling ingredient.process() repeatedly (which performs a
        // full one-time consume) and avoids double-consumption.
        try {
            ingredient.processTick(level, storages, state);
        } catch (Throwable ignored) { }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
        ingredient.ditchRecipe(level, storages, state);
    }

    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        SlotGridEntry slot = grid.next();
        var rSlot = builder.addSlot(RecipeIngredientRole.INPUT, slot.getInnerX(), slot.getInnerY());
        slot.setUsed();
        // if underlying ingredient is an item with a count, store that count on the slot for JEI rendering
        try {
            if (ingredient instanceof io.ticticboom.mods.mm.port.item.BaseItemPortIngredient bif) {
                int cnt = bif.getCount();
                slot.setBadgeCount(cnt);
                if (cnt > 1) {
                    rSlot.addRichTooltipCallback((v, list) ->
                        list.add(Component.literal("x " + cnt).withStyle(ChatFormatting.GRAY)));
                }
            }
        } catch (Throwable ignored) {
        }
        // If chance is zero (or negative), mark the slot so JEI can render a small 'x' badge
        if (chance <= 0.0) {
            slot.setBadgeNotUsed();
            // Add a translated tooltip line to indicate the slot/item won't be used
            rSlot.addTooltipCallback((v, list) -> list.add(Component.translatable("jei.mm.not_used").withStyle(ChatFormatting.DARK_AQUA)));
        }
        ingredient.setRecipe(builder, model, focus, helpers, grid, rSlot);
        var fmtChance = String.format("%.2f", chance * 100) + "% Chance of Consumption";
        rSlot.addTooltipCallback((v, list) -> {
            if (chance < 1) {
                list.add(Component.literal(fmtChance).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.literal("Consumed Per Tick").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        json.add("ingredient", ingredient.debugInput(level, storages, new JsonObject()));
        return json;
    }
}
