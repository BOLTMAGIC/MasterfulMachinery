package io.ticticboom.mods.mm.port.nuclear.radiation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;

import java.util.ArrayList;
import java.util.List;

public record NuclearRadiationPortStorageModel(
        double capacity,
        List<String> isotopes,
        boolean decay,
        boolean shielded,
        List<String> carriers,
        double loadPerItem
) implements IPortStorageModel {

    public static final double DEFAULT_LOAD_PER_ITEM = 1.0e12;

    public boolean accepts(String isotope) {
        return isotopes.isEmpty() || isotopes.contains(isotope);
    }

    public static NuclearRadiationPortStorageModel parse(JsonObject json) {
        double capacity = json.get("capacity").getAsDouble();
        boolean decay = !json.has("decay") || json.get("decay").getAsBoolean();
        boolean shielded = !json.has("shielded") || json.get("shielded").getAsBoolean();
        double loadPerItem = json.has("loadPerItem") ? json.get("loadPerItem").getAsDouble() : DEFAULT_LOAD_PER_ITEM;
        return new NuclearRadiationPortStorageModel(capacity, stringList(json, "isotopes"), decay, shielded,
                stringList(json, "carriers"), loadPerItem);
    }

    private static List<String> stringList(JsonObject json, String key) {
        var result = new ArrayList<String>();
        if (json.has(key)) {
            JsonElement element = json.get(key);
            if (element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    result.add(entry.getAsString());
                }
            } else {
                result.add(element.getAsString());
            }
        }
        return List.copyOf(result);
    }

    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", capacity);
        var isotopeList = new JsonArray();
        isotopes.forEach(isotopeList::add);
        json.add("isotopes", isotopeList);
        json.addProperty("decay", decay);
        json.addProperty("shielded", shielded);
        var carrierList = new JsonArray();
        carriers.forEach(carrierList::add);
        json.add("carriers", carrierList);
        json.addProperty("loadPerItem", loadPerItem);
        return json;
    }
}
