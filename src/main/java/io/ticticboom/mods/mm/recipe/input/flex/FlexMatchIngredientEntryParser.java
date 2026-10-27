package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.flex.FlexItems;
import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;

public class FlexMatchIngredientEntryParser implements IRecipeIngredientEntryParser {
    @Override
    public IRecipeIngredientEntry parse(JsonObject json) {
        if (!json.has("from") || json.get("from").getAsString().isBlank()) {
            throw new RuntimeException("Missing 'from' (the 'as' name of an mm:input/flex entry) in flex match input entry: " + json);
        }
        String item = json.has("item") ? json.get("item").getAsString() : null;
        var template = FlexTemplate.parse(json.has("match") ? json.get("match").getAsString() : null, item);
        FlexItems.validate(item);
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        double chance = json.has("chance") ? json.get("chance").getAsDouble() : 1.0;
        return new FlexMatchIngredientEntry(json.get("from").getAsString(), template, count, chance);
    }
}
