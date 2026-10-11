package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;

public class FlexRecipeIngredientEntryParser implements IRecipeIngredientEntryParser {
    @Override
    public IRecipeIngredientEntry parse(JsonObject json) {
        return new FlexRecipeIngredientEntry(parseName(json, "flex input entry"), parseItemIngredient(json, "flex input entry"));
    }

    /** Reads the entry's "as" name, which outputs refer to. */
    public static String parseName(JsonObject json, String context) {
        if (!json.has("as") || json.get("as").getAsString().isBlank()) {
            throw new RuntimeException("Missing 'as' name in " + context + ": " + json);
        }
        return json.get("as").getAsString();
    }

    /** Reads the entry's "ingredient", which must be an item (single item or tag). */
    public static BaseItemPortIngredient parseItemIngredient(JsonObject json, String context) {
        JsonElement ingredientEl = json.get("ingredient");
        if (ingredientEl == null || ingredientEl.isJsonNull()) {
            throw new RuntimeException("Missing or null 'ingredient' field in " + context + ": " + json);
        }
        if (!(MMPortRegistry.parseIngredient(ingredientEl) instanceof BaseItemPortIngredient item)) {
            throw new RuntimeException("The ingredient of a " + context + " must be an item (mm:item with 'item' or 'tag'): " + json);
        }
        return item;
    }
}
