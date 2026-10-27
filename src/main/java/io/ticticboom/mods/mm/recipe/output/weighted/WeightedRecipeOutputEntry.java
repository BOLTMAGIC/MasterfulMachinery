package io.ticticboom.mods.mm.recipe.output.weighted;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ChanceUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRecipeOutputEntry implements IRecipeOutputEntry {

    public record Option(@Nullable IPortIngredient ingredient, int weight) {
    }

    private final List<Option> options;
    private final int totalWeight;
    private final double chance;
    private final String chanceRollKey;
    private final String pickRollKey;

    public WeightedRecipeOutputEntry(List<Option> options, double chance) {
        this.options = options;
        this.totalWeight = options.stream().mapToInt(Option::weight).sum();
        this.chance = chance;
        String key = AmountRange.nextRollKey();
        this.chanceRollKey = "wc" + key;
        this.pickRollKey = "wp" + key;
    }

    @Nullable
    private IPortIngredient picked(@Nullable RecipeStateModel state) {
        double chanceRoll = state == null ? ThreadLocalRandom.current().nextDouble() : state.getRollToken(chanceRollKey);
        if (chance < 1 && chanceRoll >= chance) {
            return null;
        }
        double pickRoll = state == null ? ThreadLocalRandom.current().nextDouble() : state.getRollToken(pickRollKey);
        int roll = (int) Math.floor(pickRoll * totalWeight);
        for (Option option : options) {
            roll -= option.weight();
            if (roll < 0) {
                return option.ingredient();
            }
        }
        return options.get(options.size() - 1).ingredient();
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        var ingredient = picked(state);
        return ingredient == null || ingredient.canOutput(level, storages, state);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var ingredient = picked(state);
        if (ingredient != null) {
            ingredient.output(level, storages, state);
        }
    }

    @Override
    public List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        var ingredient = picked(state);
        return ingredient == null ? List.of() : List.of(ingredient);
    }

    @Override
    public List<DisplayedOutput> displayedOutputs() {
        return options.stream()
                .filter(o -> o.ingredient() != null)
                .map(o -> new DisplayedOutput(o.ingredient(), chance * o.weight() / totalWeight))
                .toList();
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
        String nothingPercent = ChanceUtils.formatPercent(chance * nothingWeight / totalWeight + (1 - chance));
        boolean hasNothing = nothingWeight > 0 || chance < 1;
        for (Option option : options) {
            if (option.ingredient() == null) {
                continue;
            }
            var rSlot = SimpleRecipeOutputEntry.addOutputSlot(builder, model, focus, helpers, grid, option.ingredient());
            String percent = ChanceUtils.formatPercent(chance * option.weight() / totalWeight);
            rSlot.addRichTooltipCallback((v, list) -> {
                list.add(Component.translatable("jei.mm.recipe.weighted_output", percent).withStyle(ChatFormatting.DARK_AQUA));
                if (hasNothing) {
                    list.add(Component.translatable("jei.mm.recipe.weighted_nothing", nothingPercent).withStyle(ChatFormatting.GRAY));
                }
            });
        }
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
