package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.function.Predicate;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;

public class FlexRecipeIngredientEntryParser implements IRecipeIngredientEntryParser {
    @Override
    public IRecipeIngredientEntry parse(JsonObject json) {
        int maxTypes = json.has("max_types") ? json.get("max_types").getAsInt() : Integer.MAX_VALUE;
        if (maxTypes < 1) {
            throw new RuntimeException("'max_types' of a flex input entry must be at least 1: " + json);
        }
        double chance = json.has("chance") ? json.get("chance").getAsDouble() : 1.0;
        var exclude = new ArrayList<Predicate<ItemStack>>();
        if (json.has("exclude")) {
            for (JsonElement e : json.getAsJsonArray("exclude")) {
                exclude.add(parseExclude(e.getAsString()));
            }
        }
        Integer minTier = json.has("min_tier") ? json.get("min_tier").getAsInt() : null;
        Integer maxTier = json.has("max_tier") ? json.get("max_tier").getAsInt() : null;
        return new FlexRecipeIngredientEntry(parseName(json, "flex input entry"), parseItemIngredient(json, "flex input entry"),
                maxTypes, chance, exclude, minTier, maxTier);
    }

    /** "modid:item", "#tag" or "modid:*". */
    private static Predicate<ItemStack> parseExclude(String text) {
        String value = text.trim();
        if (value.startsWith("#")) {
            var tag = ItemTags.create(parseId(value.substring(1), text));
            return stack -> stack.is(tag);
        }
        if (value.endsWith(":*")) {
            String namespace = value.substring(0, value.length() - 2);
            return stack -> {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                return id != null && id.getNamespace().equals(namespace);
            };
        }
        ResourceLocation id = parseId(value, text);
        return stack -> id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static ResourceLocation parseId(String value, String text) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new RuntimeException("Flex input 'exclude' entry '" + text + "' should be an item id, a #tag or modid:*");
        }
        return id;
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
