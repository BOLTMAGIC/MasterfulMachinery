package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import io.ticticboom.mods.mm.recipe.flex.RecipeMode;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexMatchIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RecipeModel(
        ResourceLocation id,
        ResourceLocation structureId,
        int ticks,
        RecipeInputs inputs,
        RecipeOutputs outputs,
        List<SlotGridEntry> inputSlots,
        List<SlotGridEntry> outputSlots,
        RecipeConditions conditions,
        boolean parallelProcessing,
        JsonObject config
) {
    public static RecipeModel parse(JsonObject json, ResourceLocation id) {
        var structrueId = ParserUtils.parseId(json, "structureId");
        var ticks = json.get("ticks").getAsInt();
        var inputs = RecipeInputs.parse(json.getAsJsonArray("inputs"));
        var outputs = RecipeOutputs.parse(json.getAsJsonArray("outputs"));

        var conditions = ParserUtils.parseOrDefault(json, "conditions",
                () -> new RecipeConditions(List.of()),
                (j) -> RecipeConditions.parse(j.getAsJsonArray("conditions")));
        var parallelProcessing = json.has("parallelProcessing") && json.get("parallelProcessing").isJsonPrimitive() ? json.get("parallelProcessing").getAsBoolean() : MMConfig.PARALLEL_PROCESSING_DEFAULT;
        try {
            linkEntries(id, RecipeMode.parse(json), FlexCost.parse(json), inputs, outputs);
        } catch (RuntimeException e) {
            throw new RuntimeException("MM recipe " + id + ": " + e.getMessage(), e);
        }
        return new RecipeModel(id, structrueId, ticks, inputs, outputs, new ArrayList<>(), new ArrayList<>(), conditions, parallelProcessing, json);
    }

    /**
     * Checks the mode and the names that entries refer to, and connects flex outputs to their flex input.
     */
    private static void linkEntries(ResourceLocation id, RecipeMode mode, @Nullable FlexCost cost, RecipeInputs inputs, RecipeOutputs outputs) {
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

    /**
     * The structures this recipe runs in: "structureId", plus any listed in "structureIds".
     */
    public List<ResourceLocation> allStructureIds() {
        var ids = new ArrayList<ResourceLocation>();
        ids.add(structureId);
        if (config != null && config.has("structureIds") && config.get("structureIds").isJsonArray()) {
            for (JsonElement e : config.getAsJsonArray("structureIds")) {
                var id = ResourceLocation.tryParse(e.getAsString());
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public String debugPath() {
        return id.getNamespace() + "/" + id.getPath() + ".json";
    }

    public boolean canProcess(Level level, RecipeStateModel model, RecipeStorages storages) {
        var canRun = conditions.canRun(new RecipeConditionContext(level, null, null));
        if (!canRun) {
            model.setTickProgress(0);
            model.setCanProcess(false);
            return false;
        }
        //inputs first, if no inputs then quit early
        boolean canProcessResult = inputs.canProcess(level, storages, model) && outputs.canProcess(level, storages, model);
        if (!canProcessResult) {
            model.setTickProgress(0);
        }
        model.setCanProcess(canProcessResult);
        return canProcessResult;
    }

    public void runTick(Level level, RecipeStateModel model, RecipeStorages storages) {
        inputs.processTick(level, storages, model);
        outputs.processTick(level, storages, model);

        model.proceedTick();
        model.setTickPercentage(((double) model.getTickProgress() / ticks) * 100);
        if (model.getTickProgress() >= ticks) {
            model.setCanFinish(true);
        }
    }

    public void process(Level level, RecipeStateModel model, RecipeStorages storages) {
        inputs.process(level, storages, model);
        outputs.process(level, storages, model);

        model.setTickProgress(0);
        model.setCanFinish(false);
    }

    public void ditchRecipe(Level level, RecipeStateModel model, RecipeStorages storages) {
        inputs.ditch(level, storages, model);
        outputs.ditch(level, storages, model);
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
