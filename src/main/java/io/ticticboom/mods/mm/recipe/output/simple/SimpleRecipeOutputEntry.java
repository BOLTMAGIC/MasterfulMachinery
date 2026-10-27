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

    public SimpleRecipeOutputEntry(IPortIngredient ingredient, double chance, boolean perTick) {

        this.ingredient = ingredient;
        this.chance = chance;
        this.perTick = perTick;
        this.chanceRollKey = "c" + AmountRange.nextRollKey();
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
        return ingredient.canOutput(level, storages, state);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun(state)) {
            ingredient.output(level, storages, state);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (perTick && shouldRun(state)) {
            ingredient.output(level, storages, state);
        }
        ingredient.outputTick(level, storages, state);
    }

    @Override
    public java.util.List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        return !perTick && shouldRun(state) ? java.util.List.of(ingredient) : java.util.List.of();
    }

    @Override
    public java.util.List<IPortIngredient> plannedTickOutputs(Level level, RecipeStateModel state) {
        // a per-tick chance is rolled again on every tick, so room is kept as if it always hits
        return perTick ? java.util.List.of(ingredient) : java.util.List.of();
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
        json.add("ingredient", ingredient.debugOutput(level, storages, new JsonObject()));
        return json;
    }
}
