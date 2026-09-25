package io.ticticboom.mods.mm.recipe.output.weighted;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Outputs exactly one of several options, picked by weight each time the recipe finishes (like a loot table).
 * An option without an ingredient outputs nothing.
 */
public class WeightedRecipeOutputEntry implements IRecipeOutputEntry {

    public record Option(@Nullable IPortIngredient ingredient, int weight) {
    }

    private final List<Option> options;
    private final int totalWeight;
    private final double chance;

    // picked in canOutput and used by output, which the controller calls right after it in the same tick
    // (the same pattern as SimpleRecipeOutputEntry#shouldRun)
    @Nullable
    private Option picked;

    public WeightedRecipeOutputEntry(List<Option> options, double chance) {
        this.options = options;
        this.totalWeight = options.stream().mapToInt(Option::weight).sum();
        this.chance = chance;
    }

    private Option pick() {
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Option option : options) {
            roll -= option.weight();
            if (roll < 0) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        picked = ChanceUtils.shouldProceed(chance) ? pick() : null;
        if (picked == null || picked.ingredient() == null) {
            return true;
        }
        return picked.ingredient().canOutput(level, storages, state);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (picked != null && picked.ingredient() != null) {
            picked.ingredient().output(level, storages, state);
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
        for (Option option : options) {
            if (option.ingredient() != null) {
                option.ingredient().ditchRecipe(level, storages, state);
            }
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        double nothingWeight = options.stream().filter(o -> o.ingredient() == null).mapToInt(Option::weight).sum();
        String nothingPercent = formatPercent(chance * nothingWeight / totalWeight + (1 - chance));
        boolean hasNothing = nothingWeight > 0 || chance < 1;
        for (Option option : options) {
            if (option.ingredient() == null) {
                continue;
            }
            var rSlot = SimpleRecipeOutputEntry.addOutputSlot(builder, model, focus, helpers, grid, option.ingredient());
            String percent = formatPercent(chance * option.weight() / totalWeight);
            rSlot.addRichTooltipCallback((v, list) -> {
                list.add(Component.translatable("jei.mm.recipe.weighted_output", percent).withStyle(ChatFormatting.DARK_AQUA));
                if (hasNothing) {
                    list.add(Component.translatable("jei.mm.recipe.weighted_nothing", nothingPercent).withStyle(ChatFormatting.GRAY));
                }
            });
        }
    }

    private static String formatPercent(double fraction) {
        return new BigDecimal(Double.toString(fraction * 100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("chance", chance);
        var optionsJson = new JsonArray();
        for (Option option : options) {
            var optionJson = new JsonObject();
            optionJson.addProperty("weight", option.weight());
            if (option.ingredient() != null) {
                optionJson.add("ingredient", option.ingredient().debugOutput(level, storages, new JsonObject()));
            }
            optionsJson.add(optionJson);
        }
        json.add("options", optionsJson);
        return json;
    }
}
