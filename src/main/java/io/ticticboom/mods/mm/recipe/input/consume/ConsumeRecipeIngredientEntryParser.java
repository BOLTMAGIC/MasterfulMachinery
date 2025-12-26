package io.ticticboom.mods.mm.recipe.input.consume;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;

public class ConsumeRecipeIngredientEntryParser implements IRecipeIngredientEntryParser {
    @Override
    public IRecipeIngredientEntry parse(JsonObject json) {
        JsonElement ingredientEl = json.get("ingredient");
        if (ingredientEl == null || ingredientEl.isJsonNull()) {
            throw new RuntimeException("Missing or null 'ingredient' field in consume input entry: " + json);
        }
        var ingredient = MMPortRegistry.parseIngredient(ingredientEl);
        double chance = 1.f;
        if (json.has("chance")) {
            chance = json.get("chance").getAsDouble();
        }
        boolean perTick = false;
        if (json.has("per_tick")) {
            perTick = json.get("per_tick").getAsBoolean();
        }
        return new ConsumeRecipeIngredientEntry(ingredient, chance, perTick);
    }
}
