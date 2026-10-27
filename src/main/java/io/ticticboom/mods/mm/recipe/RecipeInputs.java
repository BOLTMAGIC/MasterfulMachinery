package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record RecipeInputs(
        List<IRecipeIngredientEntry> inputs
) {
    public static RecipeInputs parse(JsonArray json) {
        var inputs = new ArrayList<IRecipeIngredientEntry>();
        for (JsonElement elem : json) {
            JsonObject entryJson = elem.getAsJsonObject();
            inputs.add(MachineRecipeManager.parseIngredientEntry(entryJson));
        }
        return new RecipeInputs(inputs);
    }

    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel model) {
        // flex and count inputs come first (RecipeModel orders them), so the other inputs see the run's cost multiplier
        int firstCost = 0;
        while (firstCost < inputs.size() && isSelecting(inputs.get(firstCost))) {
            if (!inputs.get(firstCost).canProcess(level, storages, model)) {
                return false;
            }
            firstCost++;
        }
        int i = firstCost;
        while (i < inputs.size()) {
            if (inputs.get(i).canProcess(level, storages, model)) {
                i++;
                continue;
            }
            // can't pay this input for every kind the flex inputs took: try again with one kind less
            if (!dropOneKind(model)) {
                return false;
            }
            i = firstCost;
        }
        return true;
    }

    private static boolean isSelecting(IRecipeIngredientEntry input) {
        return input instanceof FlexRecipeIngredientEntry || input instanceof CountRecipeIngredientEntry;
    }

    private boolean dropOneKind(RecipeStateModel model) {
        for (int i = inputs.size() - 1; i >= 0; i--) {
            if (inputs.get(i) instanceof FlexRecipeIngredientEntry flex && flex.dropLastKind(model)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlex() {
        return inputs.stream().anyMatch(input -> input instanceof FlexRecipeIngredientEntry);
    }

    public void processTick(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeIngredientEntry input : inputs) {
            input.processTick(level, storages, model);
        }
    }

    public void process(Level level, RecipeStorages storages, RecipeStateModel model) {
        if (hasFlex() && model.getCaptures().isEmpty()) {
            // a fresh run: pick the kinds (and how many the other inputs can pay for) before consuming anything
            canProcess(level, storages, model);
        }
        for (IRecipeIngredientEntry input : inputs) {
            input.process(level, storages, model);
        }
    }

    public void ditch(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeIngredientEntry input : inputs) {
            input.ditchRecipe(level, storages, model);
        }
    }

    public JsonArray debugRun(Level level, RecipeStorages storages, RecipeStateModel model) {
        var jsonArray = new JsonArray();
        for (IRecipeIngredientEntry input : inputs) {
            var expected = input.debugExpected(level, storages, model, new JsonObject());
            var inputRunJson = new JsonObject();
            inputRunJson.add("expected", expected);
            jsonArray.add(inputRunJson);
        }
        return jsonArray;
    }
}
