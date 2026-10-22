package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
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
        // each output checked the free space alone: outputs sharing an empty slot have to fit together, or the
        // last ones would be thrown away when the recipe finishes
        var items = new ArrayList<SingleItemPortIngredient>();
        for (IRecipeOutputEntry output : outputs) {
            items.addAll(output.plannedItemOutputs(level, model));
        }
        return items.size() < 2 || new OutputSimulation(storages).tryAdd(level, items);
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
