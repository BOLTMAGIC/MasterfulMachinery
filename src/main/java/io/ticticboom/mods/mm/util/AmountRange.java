package io.ticticboom.mods.mm.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;

public record AmountRange(int min, int max, String rollKey) {

    private static final ThreadLocal<int[]> KEY_COUNTER = ThreadLocal.withInitial(() -> new int[1]);

    public static void resetKeyAllocation() {
        KEY_COUNTER.get()[0] = 0;
    }

    public static String nextRollKey() {
        var counter = KEY_COUNTER.get();
        return "r" + counter[0]++;
    }

    public static AmountRange of(int amount) {
        return new AmountRange(amount, amount, null);
    }

    public static AmountRange parse(JsonObject json, String field) {
        var el = json.get(field);
        if (el == null || el.isJsonNull()) {
            throw new RuntimeException(String.format("Missing '%s' on an MM recipe ingredient: %s", field, json));
        }
        return parse(el, field);
    }

    private static AmountRange parse(JsonElement el, String field) {
        if (el.isJsonPrimitive()) {
            return of(el.getAsInt());
        }
        if (el.isJsonObject()) {
            var obj = el.getAsJsonObject();
            if (!obj.has("min") || !obj.has("max")) {
                throw new RuntimeException(String.format(
                        "Ranged '%s' on an MM recipe ingredient needs both 'min' and 'max': %s", field, obj));
            }
            int min = obj.get("min").getAsInt();
            int max = obj.get("max").getAsInt();
            if (max < min) {
                throw new RuntimeException(String.format(
                        "Ranged '%s' on an MM recipe ingredient has max (%s) below min (%s)", field, max, min));
            }
            if (min == max) {
                return of(min);
            }
            String group = obj.has("rollGroup") && !obj.get("rollGroup").isJsonNull()
                    ? obj.get("rollGroup").getAsString()
                    : null;
            return new AmountRange(min, max, group != null ? "g:" + group : nextRollKey());
        }
        throw new RuntimeException(String.format(
                "'%s' on an MM recipe ingredient must be a number or a {min, max} object, got: %s", field, el));
    }

    /**
     * This range with min and max multiplied by {@code factor} (rounded up), keeping the roll key so a scaled copy
     * rolls the same value as the original.
     */
    public AmountRange scaled(double factor) {
        int scaledMin = scaleAmount(min, factor);
        int scaledMax = scaleAmount(max, factor);
        return scaledMin == scaledMax ? of(scaledMin) : new AmountRange(scaledMin, scaledMax, rollKey);
    }

    /** Multiplies an amount, rounded up and clamped to int range. */
    public static int scaleAmount(int amount, double factor) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, Math.ceil(amount * factor)));
    }

    public boolean isRanged() {
        return max > min;
    }

    public boolean isGrouped() {
        return rollKey != null && rollKey.startsWith("g:");
    }

    public String groupName() {
        return isGrouped() ? rollKey.substring(2) : null;
    }

    public int resolve(RecipeStateModel state) {
        if (!isRanged()) {
            return min;
        }
        if (state == null) {
            return max;
        }
        double token = state.getRollToken(rollKey);
        int value = min + (int) (token * (max - min + 1));
        return Math.max(min, Math.min(max, value));
    }

    public void addToDebug(JsonObject json, String field, RecipeStateModel state) {
        json.addProperty(field, resolve(state));
        if (isRanged()) {
            var range = new JsonObject();
            range.addProperty("min", min);
            range.addProperty("max", max);
            range.addProperty("rollKey", rollKey);
            json.add(field + "Range", range);
        }
    }

    @Override
    public String toString() {
        return isRanged() ? min + "-" + max : Integer.toString(min);
    }
}
