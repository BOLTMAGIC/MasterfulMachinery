package io.ticticboom.mods.mm.recipe.input.count;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntryParser;

public class CountRecipeIngredientEntryParser implements IRecipeIngredientEntryParser {
    @Override
    public IRecipeIngredientEntry parse(JsonObject json) {
        String name = FlexRecipeIngredientEntryParser.parseName(json, "count input entry");
        var ingredient = FlexRecipeIngredientEntryParser.parseItemIngredient(json, "count input entry");
        int max = json.has("max") ? json.get("max").getAsInt() : Integer.MAX_VALUE;
        if (max < 1) {
            throw new RuntimeException("'max' of a count input entry must be at least 1: " + json);
        }
        return new CountRecipeIngredientEntry(name, ingredient, max);
    }
}
