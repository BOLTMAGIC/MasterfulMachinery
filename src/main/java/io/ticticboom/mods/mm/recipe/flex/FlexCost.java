package io.ticticboom.mods.mm.recipe.flex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * How the other consumed inputs of a flex recipe (energy, fluids, items...) grow with the number of item kinds
 * one run takes. The default comes from mm-common.toml [flex]; a recipe can override it with "flex_cost":
 * <pre>
 * "flex_cost": "once"
 * "flex_cost": { "mode": "table", "table": { "1": 1, "4": 2, "7": 3.5 } }
 * </pre>
 *
 * @param table for {@link Mode#TABLE}: kinds -> multiplier; a run uses the step with the most kinds not above its own
 */
public record FlexCost(Mode mode, NavigableMap<Integer, Double> table) {

    public enum Mode {
        /** cost x kinds: 10 kinds cost 10 times one kind */
        LINEAR,
        /** the same cost for any number of kinds */
        ONCE,
        /** the multiplier is read from the table */
        TABLE
    }

    public static final FlexCost LINEAR = new FlexCost(Mode.LINEAR, new TreeMap<>());

    /** The multiplier for the other consumed inputs of a run that takes this many kinds. */
    public double multiplier(int kinds) {
        int n = Math.max(1, kinds);
        return switch (mode) {
            case LINEAR -> n;
            case ONCE -> 1;
            case TABLE -> {
                var step = table.floorEntry(n);
                if (step != null) yield step.getValue();
                yield table.isEmpty() ? 1 : table.firstEntry().getValue();
            }
        };
    }

    /** The default from mm-common.toml [flex]. */
    public static FlexCost global() {
        return MMConfig.FLEX_COST;
    }

    public static Mode parseMode(String text) {
        try {
            return Mode.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown flex cost mode '" + text + "', expected 'linear', 'once' or 'table'");
        }
    }

    /** Whether a config table step reads as "kinds=multiplier". */
    public static boolean isStep(String step) {
        try {
            parseStep(step, new TreeMap<>());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void parseStep(String step, NavigableMap<Integer, Double> into) {
        String[] parts = step.split("=");
        if (parts.length != 2) {
            throw new RuntimeException("Flex cost step '" + step + "' should look like \"kinds=multiplier\", e.g. \"4=2\"");
        }
        int kinds;
        double multiplier;
        try {
            kinds = Integer.parseInt(parts[0].trim());
            multiplier = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Flex cost step '" + step + "' should look like \"kinds=multiplier\", e.g. \"4=2\"");
        }
        if (kinds < 1 || multiplier < 0) {
            throw new RuntimeException("Flex cost step '" + step + "': kinds must be at least 1 and the multiplier not negative");
        }
        into.put(kinds, multiplier);
    }

    /** Builds a cost from the config values. */
    public static FlexCost of(Mode mode, List<? extends String> steps) {
        var table = new TreeMap<Integer, Double>();
        for (String step : steps) {
            parseStep(step, table);
        }
        return new FlexCost(mode, table);
    }

    /**
     * Reads a recipe's "flex_cost".
     *
     * @return null if the recipe has none (the config default is used)
     */
    @Nullable
    public static FlexCost parse(JsonObject recipe) {
        JsonElement el = recipe.get("flex_cost");
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            return new FlexCost(parseMode(el.getAsString()), new TreeMap<>());
        }
        JsonObject json = el.getAsJsonObject();
        Mode mode = json.has("mode") ? parseMode(json.get("mode").getAsString()) : Mode.TABLE;
        var steps = new ArrayList<String>();
        if (json.has("table")) {
            JsonElement tableEl = json.get("table");
            if (tableEl.isJsonArray()) {
                tableEl.getAsJsonArray().forEach(e -> steps.add(e.getAsString()));
            } else {
                for (Map.Entry<String, JsonElement> e : tableEl.getAsJsonObject().entrySet()) {
                    steps.add(e.getKey() + "=" + e.getValue().getAsString());
                }
            }
        }
        if (mode == Mode.TABLE && steps.isEmpty()) {
            throw new RuntimeException("\"flex_cost\" with mode 'table' needs a 'table', e.g. {\"1\": 1, \"4\": 2}");
        }
        return of(mode, steps);
    }
}
