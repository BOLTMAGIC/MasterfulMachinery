package io.ticticboom.mods.mm.recipe.flex;

import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * How a recipe's items are defined. "fixed" (the default) lists every input and output; "flex" takes any item
 * matching an "mm:input/flex" entry and works out the output from it ("mm:output/flex").
 */
public enum RecipeMode {
    FIXED,
    FLEX;

    public static RecipeMode parse(JsonObject json) {
        if (!json.has("mode") || json.get("mode").isJsonNull()) {
            return FIXED;
        }
        String text = json.get("mode").getAsString();
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown recipe mode '" + text + "', expected 'fixed' or 'flex'");
        }
    }
}
