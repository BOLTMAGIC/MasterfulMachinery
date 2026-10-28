package io.ticticboom.mods.mm.compat.kjs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;

public final class KubeJsValues {

    private KubeJsValues() {
    }

    public static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JsonElement json) {
            return json;
        }
        if (value instanceof CharSequence text) {
            return new JsonPrimitive(text.toString());
        }
        if (value instanceof Boolean flag) {
            return new JsonPrimitive(flag);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof List<?> list) {
            var array = new JsonArray();
            for (var entry : list) {
                array.add(toJson(entry));
            }
            return array;
        }
        if (value instanceof Map<?, ?> map) {
            var object = new JsonObject();
            map.forEach((key, entry) -> object.add(String.valueOf(key), toJson(entry)));
            return object;
        }
        return new JsonPrimitive(String.valueOf(value));
    }
}
