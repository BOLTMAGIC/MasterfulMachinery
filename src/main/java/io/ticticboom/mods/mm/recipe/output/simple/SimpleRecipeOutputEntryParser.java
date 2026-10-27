package io.ticticboom.mods.mm.recipe.output.simple;

import io.ticticboom.mods.mm.port.IPortIngredient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntryParser;
import io.ticticboom.mods.mm.Ref;

public class SimpleRecipeOutputEntryParser implements IRecipeOutputEntryParser {
    @Override
    public IRecipeOutputEntry parse(JsonObject json) {
        var ingredient = parseIngredient(json, "simple output entry");
        double chance = 1.f;
        if (json.has("chance")) {
            chance = json.get("chance").getAsDouble();
        }
        boolean perTick = false;
        if (json.has("per_tick")) {
            perTick = json.get("per_tick").getAsBoolean();
        }
        String scaleBy = null;
        if (json.has("scale_by")) {
            scaleBy = json.get("scale_by").getAsString();
            if (ingredient.scaled(1) == null) {
                throw new RuntimeException("This output can't use 'scale_by' (only item, fluid, energy and Mekanism chemical outputs can): " + json);
            }
        }
        return new SimpleRecipeOutputEntry(ingredient, chance, perTick, scaleBy);
    }

    public static IPortIngredient parseIngredient(JsonObject json, String what) {
        JsonElement ingredientEl = json.get("ingredient");
        if (ingredientEl == null || ingredientEl.isJsonNull()) {
            if (json.has("item") || json.has("nbt") || json.has("nbt_snbt") || json.has("nbt_match") || json.has("count")) {
                JsonObject normalized = new JsonObject();
                normalized.addProperty("type", Ref.Ports.ITEM.toString());
                if (json.has("item")) normalized.add("item", json.get("item"));
                if (json.has("count")) normalized.add("count", json.get("count"));
                if (json.has("nbt")) normalized.add("nbt", json.get("nbt"));
                if (json.has("nbt_snbt")) normalized.add("nbt_snbt", json.get("nbt_snbt"));
                if (json.has("nbt_match")) normalized.add("nbt_match", json.get("nbt_match"));
                ingredientEl = normalized;
            } else {
                throw new RuntimeException("Missing or null 'ingredient' field in " + what + ": " + json);
            }
        }
        return MMPortRegistry.parseIngredient(ingredientEl);
    }
}
