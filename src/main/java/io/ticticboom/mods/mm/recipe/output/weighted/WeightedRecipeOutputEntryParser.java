package io.ticticboom.mods.mm.recipe.output.weighted;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntryParser;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntryParser;

import java.util.ArrayList;
import java.util.List;

/**
 * {"type": "mm:output/weighted", "chance": 1.0, "options": [{"weight": 3, "ingredient": {...}}, {"weight": 1}]}
 * Each option takes an "ingredient" or the item shorthand fields; an option with neither outputs nothing.
 */
public class WeightedRecipeOutputEntryParser implements IRecipeOutputEntryParser {
    @Override
    public IRecipeOutputEntry parse(JsonObject json) {
        if (!json.has("options") || !json.get("options").isJsonArray() || json.getAsJsonArray("options").isEmpty()) {
            throw new RuntimeException("Weighted output entry needs a non-empty 'options' array: " + json);
        }
        List<WeightedRecipeOutputEntry.Option> options = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("options")) {
            JsonObject optionJson = element.getAsJsonObject();
            int weight = optionJson.has("weight") ? optionJson.get("weight").getAsInt() : 1;
            if (weight <= 0) {
                throw new RuntimeException("Weighted output option weight must be positive: " + optionJson);
            }
            boolean nothing = !optionJson.has("ingredient") && !optionJson.has("item");
            options.add(new WeightedRecipeOutputEntry.Option(
                    nothing ? null : SimpleRecipeOutputEntryParser.parseIngredient(optionJson, "weighted output option"), weight));
        }
        double chance = json.has("chance") ? json.get("chance").getAsDouble() : 1;
        return new WeightedRecipeOutputEntry(options, chance);
    }
}
