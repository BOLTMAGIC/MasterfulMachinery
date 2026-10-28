package io.ticticboom.mods.mm.port.ae2.pattern;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.ae2.Ae2KeyBridges;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;

import java.util.HashSet;
import java.util.Set;

public class Ae2PatternPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var priority = json.has("patternPriority") ? json.get("patternPriority").getAsInt() : 0;
        var exclude = new HashSet<String>();
        var raw = json.get("exclude");
        if (raw != null && raw.isJsonArray()) {
            raw.getAsJsonArray().forEach(element -> exclude.add(element.getAsString()));
        } else if (raw != null && raw.isJsonPrimitive()) {
            exclude.add(raw.getAsString());
        }
        Ae2KeyBridges.warnUnknown(exclude);
        return new Ae2PatternPortStorageFactory(new Ae2PatternPortStorageModel(priority, Set.copyOf(exclude)));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        throw new RuntimeException("ME pattern ports can't be used as recipe ingredients");
    }
}
