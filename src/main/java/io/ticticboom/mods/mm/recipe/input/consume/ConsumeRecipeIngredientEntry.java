package io.ticticboom.mods.mm.recipe.input.consume;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.RecipeMode;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.util.AmountRange;
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
    @Getter
    private final double chance;
    @Getter
    private final boolean perTick;
    private final String chanceRollKey;
    private final java.util.Map<Double, IPortIngredient> scaled = new java.util.concurrent.ConcurrentHashMap<>();

    public ConsumeRecipeIngredientEntry(IPortIngredient ingredient, double chance, boolean perTick) {
        this.ingredient = ingredient;
        this.chance = chance;
        this.perTick = perTick;
        this.chanceRollKey = "c" + AmountRange.nextRollKey();
    }

    /**
     * The ingredient for this run: in flex recipes multiplied by the run's cost multiplier (the number of kinds it
     * takes, see FlexCost). Ingredients that can't be scaled keep their amount.
     */
    public IPortIngredient ingredientFor(RecipeStateModel state) {
        double multiplier = state == null ? 1 : state.getCostMultiplier();
        if (multiplier == 1) {
            return ingredient;
        }
        return scaled.computeIfAbsent(multiplier, m -> {
            IPortIngredient copy = ingredient.scaled(m);
            return copy == null ? ingredient : copy;
        });
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
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        return ingredientFor(state).canProcess(level, storages, state);
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun(state)) {
            ingredientFor(state).process(level, storages, state);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        // Call the ingredient's per-tick hook. Ingredients that are consumed per-tick
        // should implement their own processTick() to perform the actual extraction.
        // This prevents calling ingredient.process() repeatedly (which performs a
        // full one-time consume) and avoids double-consumption.
        if (!perTick) {
            return;
        }
        ingredientFor(state).processTick(level, storages, state);
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
        if (ingredient instanceof BaseItemPortIngredient item) {
            slot.setBadgeCount(item.getCount());
        }
        // If chance is zero (or negative), mark the slot so JEI can render a small 'x' badge
        if (chance <= 0.0) {
            slot.setBadgeNotUsed();
            // Add a translated tooltip line to indicate the slot/item won't be used
            rSlot.addTooltipCallback((v, list) -> list.add(Component.translatable("jei.mm.not_used").withStyle(ChatFormatting.DARK_AQUA)));
        }
        if (slot.getBadgeCount() > 1 || slot.hasBadgeNotUsed()) {
            rSlot.setOverlay(new SlotBadgeDrawable(slot.getBadgeCount(), slot.hasBadgeNotUsed()), 0, 0);
        }
        ingredient.setRecipe(builder, model, focus, helpers, grid, rSlot);
        var range = ingredient.getAmountRange();
        if (range != null && range.isRanged()) {
            rSlot.addTooltipCallback((v, list) -> {
                list.add(Component.translatable("jei.mm.recipe.consumes_range", range.min(), range.max())
                        .withStyle(ChatFormatting.DARK_AQUA));
                if (range.isGrouped()) {
                    list.add(Component.translatable("jei.mm.recipe.roll_group", range.groupName())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        }
        var fmtChance = String.format("%.2f", chance * 100);
        rSlot.addTooltipCallback((v, list) -> {
            if (chance < 1) {
                list.add(Component.translatable("jei.mm.recipe.chance_of_consumption", fmtChance).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.translatable("jei.mm.recipe.consumed_per_tick").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (model.mode() == RecipeMode.FLEX) {
                list.add(Component.translatable("jei.mm.recipe.flex_cost").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        json.addProperty("chanceRoll", shouldRun(state));
        json.addProperty("costMultiplier", state.getCostMultiplier());
        json.add("ingredient", ingredientFor(state).debugInput(level, storages, new JsonObject()));
        return json;
    }
}
