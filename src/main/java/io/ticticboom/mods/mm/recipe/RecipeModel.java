package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import io.ticticboom.mods.mm.recipe.flex.RecipeMode;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexMatchIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record RecipeModel(
        Identifier id,
        Identifier structureId,
        int ticks,
        RecipeInputs inputs,
        RecipeOutputs outputs,
        List<SlotGridEntry> inputSlots,
        List<SlotGridEntry> outputSlots,
        RecipeConditions conditions,
        boolean parallelProcessing,
        boolean requestOnly,
        JsonObject config
) {
    public static RecipeModel parse(JsonObject json, Identifier id) {
        AmountRange.resetKeyAllocation();
        var structrueId = ParserUtils.parseId(json, "structureId");
        var ticks = json.get("ticks").getAsInt();
        var inputs = RecipeInputs.parse(json.getAsJsonArray("inputs"));
        var outputs = RecipeOutputs.parse(json.getAsJsonArray("outputs"));

        var conditions = ParserUtils.parseOrDefault(json, "conditions",
                () -> new RecipeConditions(List.of()),
                (j) -> RecipeConditions.parse(j.getAsJsonArray("conditions")));
        var parallelProcessing = json.has("parallelProcessing") && json.get("parallelProcessing").isJsonPrimitive() ? json.get("parallelProcessing").getAsBoolean() : MMConfig.PARALLEL_PROCESSING_DEFAULT;
        var requestOnly = json.has("requestOnly") && json.get("requestOnly").isJsonPrimitive() && json.get("requestOnly").getAsBoolean();
        try {
            linkEntries(id, RecipeMode.parse(json), FlexCost.parse(json), inputs, outputs);
        } catch (RuntimeException e) {
            throw new RuntimeException("MM recipe " + id + ": " + e.getMessage(), e);
        }
        return new RecipeModel(id, structrueId, ticks, inputs, outputs, new ArrayList<>(), new ArrayList<>(), conditions, parallelProcessing, requestOnly, json);
    }

    /**
     * Checks the mode and the names that entries refer to, and connects flex outputs to their flex input.
     */
    private static void linkEntries(Identifier id, RecipeMode mode, @Nullable FlexCost cost, RecipeInputs inputs, RecipeOutputs outputs) {
        // count, then flex inputs first: the others are paid per run with the cost multiplier the flex inputs set
        inputs.inputs().sort(Comparator.comparingInt((IRecipeIngredientEntry input) -> input instanceof CountRecipeIngredientEntry ? 0
                : input instanceof FlexRecipeIngredientEntry ? 1 : 2));
        Map<String, FlexRecipeIngredientEntry> flexInputs = new HashMap<>();
        Set<String> counted = new HashSet<>();
        for (var input : inputs.inputs()) {
            if (input instanceof FlexRecipeIngredientEntry flex) {
                if (mode != RecipeMode.FLEX) {
                    throw new RuntimeException("mm:input/flex needs \"mode\": \"flex\" on the recipe");
                }
                if (flexInputs.put(flex.getName(), flex) != null || counted.contains(flex.getName())) {
                    throw new RuntimeException("two inputs are named '" + flex.getName() + "'");
                }
                flex.bindCost(cost);
            } else if (input instanceof CountRecipeIngredientEntry count) {
                if (!counted.add(count.getName()) || flexInputs.containsKey(count.getName())) {
                    throw new RuntimeException("two inputs are named '" + count.getName() + "'");
                }
            }
        }
        for (var input : inputs.inputs()) {
            if (input instanceof FlexMatchIngredientEntry match) {
                if (mode != RecipeMode.FLEX) {
                    throw new RuntimeException("mm:input/flex_match needs \"mode\": \"flex\" on the recipe");
                }
                var flex = flexInputs.get(match.getFrom());
                if (flex == null) {
                    throw new RuntimeException("flex match input 'from' is '" + match.getFrom() + "' but no mm:input/flex has that 'as' name");
                }
                flex.bindMatch(match);
            }
        }
        if (cost != null && mode != RecipeMode.FLEX) {
            throw new RuntimeException("\"flex_cost\" needs \"mode\": \"flex\" on the recipe");
        }
        if (mode == RecipeMode.FLEX && flexInputs.isEmpty()) {
            throw new RuntimeException("a flex recipe needs at least one mm:input/flex input");
        }
        for (var output : outputs.outputs()) {
            String scaleBy = null;
            if (output instanceof FlexRecipeOutputEntry flex) {
                if (mode != RecipeMode.FLEX) {
                    throw new RuntimeException("mm:output/flex needs \"mode\": \"flex\" on the recipe");
                }
                var input = flexInputs.get(flex.getFrom());
                if (input == null) {
                    throw new RuntimeException("flex output 'from' is '" + flex.getFrom() + "' but no mm:input/flex has that 'as' name");
                }
                flex.bind(id, input);
                input.bindOutput(flex);
                scaleBy = flex.getScaleBy();
            } else if (output instanceof SimpleRecipeOutputEntry simple) {
                scaleBy = simple.getScaleBy();
            }
            if (scaleBy != null && !counted.contains(scaleBy) && !flexInputs.containsKey(scaleBy)) {
                throw new RuntimeException("output 'scale_by' is '" + scaleBy + "' but no mm:input/count or mm:input/flex has that 'as' name");
            }
        }
    }

    public RecipeMode mode() {
        return config == null ? RecipeMode.FIXED : RecipeMode.parse(config);
    }

    public String debugPath() {
        return id.getNamespace() + "/" + id.getPath() + ".json";
    }

    public List<Identifier> allStructureIds() {
        var ids = new ArrayList<Identifier>();
        ids.add(structureId);
        if (config != null && config.has("structureIds") && config.get("structureIds").isJsonArray()) {
            for (JsonElement e : config.getAsJsonArray("structureIds")) {
                var id = Identifier.tryParse(e.getAsString());
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public boolean runsIn(Identifier structure) {
        return allStructureIds().contains(structure);
    }

    public JsonObject debugRun(Level level, RecipeStateModel model, RecipeStorages storages) {
        var json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("structureId", structureId.toString());
        json.addProperty("ticks", ticks);
        json.add("inputs", inputs.debugRun(level, storages, model));
        json.add("outputs", outputs.debugRun(level, storages, model));
        json.add("portStorages", storages.debug());
        return json;
    }
}
