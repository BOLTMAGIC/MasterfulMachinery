package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.flex.OutputSimulation;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record RecipeOutputs(
        List<IRecipeOutputEntry> outputs
) {
    public static RecipeOutputs parse(JsonArray json) {
        var outputs = new ArrayList<IRecipeOutputEntry>();
        for (JsonElement elem : json) {
            JsonObject entryJson = elem.getAsJsonObject();
            outputs.add(MachineRecipeManager.parseOutputEntry(entryJson));
        }
        return new RecipeOutputs(outputs);
    }

    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (int i = 0; i < outputs.size(); i++) {
            IRecipeOutputEntry output = outputs.get(i);
            boolean ok = output.canOutput(level, storages, model);
            if (!ok) {
                // debug suppressed
                return false;
            }
        }
        // each output checked the free space alone: outputs sharing an empty slot or tank have to fit together, or
        // the last ones would be thrown away when the recipe finishes
        var planned = new ArrayList<IPortIngredient>();
        for (IRecipeOutputEntry output : outputs) {
            planned.addAll(output.plannedOutputs(level, model));
        }
        return fitTogether(level, storages, planned);
    }

    /**
     * Whether this tick's "per_tick" outputs fit; when they don't, the recipe waits this tick (no progress, no
     * per-tick inputs taken) instead of throwing them away.
     */
    public boolean canProcessTick(Level level, RecipeStorages storages, RecipeStateModel model) {
        var planned = new ArrayList<IPortIngredient>();
        for (IRecipeOutputEntry output : outputs) {
            planned.addAll(output.plannedTickOutputs(level, model));
        }
        for (IPortIngredient output : planned) {
            if (!output.canOutput(level, storages, model)) {
                return false;
            }
        }
        return fitTogether(level, storages, planned);
    }

    /** Outputs that each fit alone: do the ones that can be simulated fit next to each other? */
    private static boolean fitTogether(Level level, RecipeStorages storages, List<IPortIngredient> outputs) {
        var simulated = outputs.stream().filter(IPortIngredient::canSimulateOutput).toList();
        return simulated.size() < 2 || new OutputSimulation(storages).tryAdd(level, simulated);
    }

    public void process(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.output(level, storages, model);
        }
    }

    public void processTick(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.processTick(level, storages, model);
        }
    }


    public void ditch(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.ditchRecipe(level, storages, model);
        }
    }


    public JsonArray debugRun(Level level, RecipeStorages storages, RecipeStateModel model) {
        var jsonArray = new JsonArray();
        for (IRecipeOutputEntry output : outputs) {
            var expected = output.debugExpected(level, storages, model, new JsonObject());
            var inputRunJson = new JsonObject();
            inputRunJson.add("expected", expected);
            jsonArray.add(inputRunJson);
        }
        return jsonArray;
    }
}
