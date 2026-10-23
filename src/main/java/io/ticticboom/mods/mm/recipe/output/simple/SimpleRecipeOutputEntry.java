package io.ticticboom.mods.mm.recipe.output.simple;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleRecipeOutputEntry implements IRecipeOutputEntry {

    @Getter
    private final IPortIngredient ingredient;
    @Getter
    private final double chance;
    @Getter
    private final boolean perTick;
    /** The "as" name of an mm:input/count entry whose count multiplies the amount, or null. */
    @Getter
    @Nullable
    private final String scaleBy;
    private final Map<Integer, IPortIngredient> scaled = new ConcurrentHashMap<>();

    private boolean shouldRun = true;

    public SimpleRecipeOutputEntry(IPortIngredient ingredient, double chance, boolean perTick) {
        this(ingredient, chance, perTick, null);
    }

    public SimpleRecipeOutputEntry(IPortIngredient ingredient, double chance, boolean perTick, @Nullable String scaleBy) {

        this.ingredient = ingredient;
        this.chance = chance;
        this.perTick = perTick;
        this.scaleBy = scaleBy;
    }

    /** The ingredient for this run: scaled by the counted items when "scale_by" is set. */
    private IPortIngredient ingredientFor(RecipeStateModel state) {
        if (scaleBy == null) {
            return ingredient;
        }
        return scaled.computeIfAbsent(state.getCount(scaleBy), f -> ingredient.scaled(f));
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        shouldRun = ChanceUtils.shouldProceed(chance);
        if (!shouldRun) {
            return true;
        }
        return ingredientFor(state).canOutput(level, storages, state);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun) {
            ingredientFor(state).output(level, storages, state);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (perTick && shouldRun) {
            ingredientFor(state).output(level, storages, state);
        }
        ingredientFor(state).outputTick(level, storages, state);
    }

    @Override
    public java.util.List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        return !perTick && shouldRun ? java.util.List.of(ingredientFor(state)) : java.util.List.of();
    }

    @Override
    public java.util.List<IPortIngredient> plannedTickOutputs(Level level, RecipeStateModel state) {
        return perTick && shouldRun ? java.util.List.of(ingredientFor(state)) : java.util.List.of();
    }

    @Override
    public java.util.List<DisplayedOutput> displayedOutputs() {
        return java.util.List.of(new DisplayedOutput(ingredient, chance));
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
        ingredient.ditchRecipe(level, storages, state);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        var rSlot = addOutputSlot(builder, model, focus, helpers, grid, ingredient);
        double percent = chance * 100.0;
        String percentStr = new java.math.BigDecimal(Double.toString(percent)).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        var fmtChance = percentStr + "% Chance of Output";
        rSlot.addRichTooltipCallback((v, list) -> {
            if (chance < 1) {
                list.add(Component.literal(fmtChance).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.translatable("jei.mm.recipe.output_per_tick").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (scaleBy != null) {
                list.add(Component.translatable("jei.mm.recipe.scaled_by", scaleBy).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    /**
     * Adds a JEI output slot on the next grid cell for the ingredient, with the item count badge.
     */
    public static IRecipeSlotBuilder addOutputSlot(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IPortIngredient ingredient) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, slot.getInnerX(), slot.getInnerY());
        // if underlying ingredient is an item, store its intended count on the slot for JEI rendering
        try {
            if (ingredient instanceof io.ticticboom.mods.mm.port.item.BaseItemPortIngredient bif) {
                int cnt = bif.getCount();
                slot.setBadgeCount(cnt);
                if (cnt > 1) {
                    rSlot.setOverlay(new SlotBadgeDrawable(cnt, false), 0, 0);
                    rSlot.addRichTooltipCallback((v, list) ->
                        list.add(Component.literal("x " + cnt).withStyle(ChatFormatting.GRAY)));
                }
            }
        } catch (Throwable ignored) {
        }
        ingredient.setRecipe(builder, model, focus, helpers, grid, rSlot);
        return rSlot;
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        if (scaleBy != null) {
            json.addProperty("scaleBy", scaleBy);
            json.addProperty("factor", model.getCount(scaleBy));
        }
        json.add("ingredient", ingredientFor(model).debugOutput(level, storages, new JsonObject()));
        return json;
    }
}
