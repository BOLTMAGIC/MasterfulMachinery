package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class RecipeConditions {
    private final List<IRecipeCondition> conditions;

    public RecipeConditions(List<IRecipeCondition> conditions) {
        this.conditions = conditions;
    }

    public static RecipeConditions parse(JsonArray arr) {
        var result = new ArrayList<IRecipeCondition>();
        for (JsonElement elem : arr) {
            JsonObject json = elem.getAsJsonObject();
            var cond = MachineRecipeManager.parseCondition(json);
            result.add(cond);
        }
        return new RecipeConditions(result);
    }

    public boolean canRun(RecipeConditionContext ctx) {
        for (IRecipeCondition condition : conditions) {
            if (!condition.canRun(ctx)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    /** One line per condition, for JEI. */
    public List<Component> describe() {
        return conditions.stream().map(IRecipeCondition::describe).toList();
    }
}
