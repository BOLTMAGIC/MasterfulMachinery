package io.ticticboom.mods.mm.recipe.output.simple;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ChanceUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class SimpleRecipeOutputEntry implements IRecipeOutputEntry {

    private final IPortIngredient ingredient;
    private final double chance;
    private final boolean perTick;
    private final String chanceRollKey;
    /** The "as" name of an mm:input/count (or mm:input/flex) entry whose number multiplies the amount, or null. */
    @org.jetbrains.annotations.Nullable
    private final String scaleBy;
    private final java.util.Map<Integer, IPortIngredient> scaled = new java.util.concurrent.ConcurrentHashMap<>();

    public SimpleRecipeOutputEntry(IPortIngredient ingredient, double chance, boolean perTick) {
        this(ingredient, chance, perTick, null);
    }

    public SimpleRecipeOutputEntry(IPortIngredient ingredient, double chance, boolean perTick, @org.jetbrains.annotations.Nullable String scaleBy) {

        this.ingredient = ingredient;
        this.chance = chance;
        this.perTick = perTick;
        this.scaleBy = scaleBy;
        this.chanceRollKey = "c" + AmountRange.nextRollKey();
    }

    public double getChance() {
        return chance;
    }

    public boolean isPerTick() {
        return perTick;
    }

    @org.jetbrains.annotations.Nullable
    public String getScaleBy() {
        return scaleBy;
    }

    /** The ingredient for this run: scaled by the counted items when "scale_by" is set. */
    private IPortIngredient ingredientFor(RecipeStateModel state) {
        if (scaleBy == null || state == null) {
            return ingredient;
        }
        return scaled.computeIfAbsent(state.getCount(scaleBy), factor -> ingredient.scaled(factor));
    }

    @Override
    public java.util.List<io.ticticboom.mods.mm.recipe.output.DisplayedOutput> displayedOutputs() {
        return java.util.List.of(new io.ticticboom.mods.mm.recipe.output.DisplayedOutput(ingredient, chance));
    }

    public IPortIngredient getIngredient() {
        return ingredient;
    }

    private boolean shouldRun(RecipeStateModel state) {
        if (chance >= 1) {
            return true;
        }
        if (perTick || state == null) {
            return ChanceUtils.shouldProceed(chance);
        }
        return chance >= state.getRollToken(chanceRollKey);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!shouldRun(state)) {
            return true;
        }
        return ingredientFor(state).canOutput(level, storages, state);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun(state)) {
            ingredientFor(state).output(level, storages, state);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (perTick && shouldRun(state)) {
            ingredientFor(state).output(level, storages, state);
        }
        ingredientFor(state).outputTick(level, storages, state);
    }

    @Override
    public java.util.List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        return !perTick && shouldRun(state) ? java.util.List.of(ingredientFor(state)) : java.util.List.of();
    }

    @Override
    public java.util.List<IPortIngredient> plannedTickOutputs(Level level, RecipeStateModel state) {
        // a per-tick chance is rolled again on every tick, so room is kept as if it always hits
        return perTick ? java.util.List.of(ingredientFor(state)) : java.util.List.of();
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
        rSlot.addRichTooltipCallback((v, list) -> {
            if (chance < 1) {
                list.add(Component.translatable("jei.mm.recipe.chance_of_output", percentStr).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.translatable("jei.mm.recipe.output_per_tick").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (scaleBy != null) {
                list.add(Component.translatable("jei.mm.recipe.scaled_by", scaleBy).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    public static IRecipeSlotBuilder addOutputSlot(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IPortIngredient ingredient) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, slot.getInnerX(), slot.getInnerY());
        if (ingredient instanceof BaseItemPortIngredient item) {
            slot.setBadgeCount(item.getCount());
            if (item.getCount() > 1) {
                rSlot.setOverlay(new SlotBadgeDrawable(item.getCount(), false), 0, 0);
            }
        }
        var range = ingredient.getAmountRange();
        if (range != null && range.isRanged()) {
            rSlot.addRichTooltipCallback((v, list) -> {
                list.add(Component.translatable("jei.mm.recipe.outputs_range", range.min(), range.max())
                        .withStyle(ChatFormatting.DARK_AQUA));
                if (range.isGrouped()) {
                    list.add(Component.translatable("jei.mm.recipe.roll_group", range.groupName())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        }
        ingredient.setRecipe(builder, model, focus, helpers, grid, rSlot);
        return rSlot;
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        json.addProperty("chanceRoll", shouldRun(model));
        if (scaleBy != null) {
            json.addProperty("scaleBy", scaleBy);
            json.addProperty("factor", model.getCount(scaleBy));
        }
        json.add("ingredient", ingredientFor(model).debugOutput(level, storages, new JsonObject()));
        return json;
    }
}
