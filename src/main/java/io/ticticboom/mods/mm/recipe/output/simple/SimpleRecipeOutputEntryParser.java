package io.ticticboom.mods.mm.recipe.output.simple;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntryParser;

public class SimpleRecipeOutputEntryParser implements IRecipeOutputEntryParser {
    @Override
    public IRecipeOutputEntry parse(JsonObject json) {
        JsonElement ingredientEl = json.get("ingredient");
        // If ingredient is missing, allow shorthand fields (item/count/nbt/nbt_snbt/nbt_match)
        if (ingredientEl == null || ingredientEl.isJsonNull()) {
            // look for direct item/count/nbt fields and normalize
            if (json.has("item") || json.has("nbt") || json.has("nbt_snbt") || json.has("nbt_match") || json.has("count")) {
                JsonObject normalized = new JsonObject();
                normalized.addProperty("type", io.ticticboom.mods.mm.Ref.Ports.ITEM.toString());
                if (json.has("item")) normalized.add("item", json.get("item"));
                if (json.has("count")) normalized.add("count", json.get("count"));
                if (json.has("nbt")) normalized.add("nbt", json.get("nbt"));
                if (json.has("nbt_snbt")) normalized.add("nbt_snbt", json.get("nbt_snbt"));
                if (json.has("nbt_match")) normalized.add("nbt_match", json.get("nbt_match"));
                ingredientEl = normalized;
            } else {
                throw new RuntimeException("Missing or null 'ingredient' field in simple output entry: " + json);
            }
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
        return new SimpleRecipeOutputEntry(ingredient, chance, perTick);
    }
}
