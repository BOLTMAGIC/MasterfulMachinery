package io.ticticboom.mods.mm.structure.attachment.states;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public record StateLists(Map<String, NamedStateList> states) {
    public NamedStateList get(String name) {
        return states.get(name);
    }

    public boolean has(String name) {
        return states.containsKey(name);
    }

    public static StateLists parse(JsonObject states) {
        var map = new HashMap<String, NamedStateList>();
        for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException(String.format("State list %s must be an object", entry.getKey()));
            }
            var obj = entry.getValue().getAsJsonObject();
            var stateList = NamedStateList.parse(entry.getKey(), obj);
            map.put(entry.getKey(), stateList);
        }
        return new StateLists(map);
    }
}
