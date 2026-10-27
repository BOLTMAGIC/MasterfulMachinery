package io.ticticboom.mods.mm.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;

public record LongAmountRange(long min, long max, String rollKey) {

    public static LongAmountRange of(long amount) {
        return new LongAmountRange(amount, amount, null);
    }

    public static LongAmountRange parse(JsonObject json, String field) {
        JsonElement element = json.get(field);
        if (element == null || element.isJsonNull()) {
            throw new RuntimeException(String.format("Missing '%s' on an MM recipe ingredient: %s", field, json));
        }
        if (element.isJsonPrimitive()) {
            return of(element.getAsLong());
        }
        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();
            if (!obj.has("min") || !obj.has("max")) {
                throw new RuntimeException(String.format(
                        "Ranged '%s' on an MM recipe ingredient needs both 'min' and 'max': %s", field, obj));
            }
            long min = obj.get("min").getAsLong();
            long max = obj.get("max").getAsLong();
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
            return new LongAmountRange(min, max, group != null ? "g:" + group : AmountRange.nextRollKey());
        }
        throw new RuntimeException(String.format(
                "'%s' on an MM recipe ingredient must be a number or a {min, max} object, got: %s", field, element));
    }

    /** Like {@link AmountRange#scaled(double)}. */
    public LongAmountRange scaled(double factor) {
        long scaledMin = scaleAmount(min, factor);
        long scaledMax = scaleAmount(max, factor);
        return scaledMin == scaledMax ? of(scaledMin) : new LongAmountRange(scaledMin, scaledMax, rollKey);
    }

    /** Multiplies an amount, rounded up and clamped to long range. */
    public static long scaleAmount(long amount, double factor) {
        return (long) Math.min(Long.MAX_VALUE, Math.max(0, Math.ceil(amount * factor)));
    }

    public boolean isRanged() {
        return max > min;
    }

    public long resolve(RecipeStateModel state) {
        if (!isRanged()) {
            return min;
        }
        if (state == null) {
            return max;
        }
        double token = state.getRollToken(rollKey);
        long value = min + (long) (token * ((double) (max - min) + 1));
        return Math.max(min, Math.min(max, value));
    }
}
