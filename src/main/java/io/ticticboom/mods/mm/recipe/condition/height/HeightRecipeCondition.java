package io.ticticboom.mods.mm.recipe.condition.height;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

/**
 * The controller's Y must be within a range: {"type": "mm:height", "minY": 0, "maxY": 64}; either end may be left out.
 */
public class HeightRecipeCondition implements IRecipeCondition {
    private final int minY;
    private final int maxY;

    public HeightRecipeCondition(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    public static IRecipeCondition parse(JsonObject json) {
        int min = json.has("minY") ? json.get("minY").getAsInt() : Integer.MIN_VALUE;
        int max = json.has("maxY") ? json.get("maxY").getAsInt() : Integer.MAX_VALUE;
        return new HeightRecipeCondition(min, max);
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.pos() == null) return true;
        int y = ctx.pos().getY();
        return y >= minY && y <= maxY;
    }

    @Override
    public Component describe() {
        if (minY == Integer.MIN_VALUE) return Component.translatable("jei.mm.condition.height.max", maxY);
        if (maxY == Integer.MAX_VALUE) return Component.translatable("jei.mm.condition.height.min", minY);
        return Component.translatable("jei.mm.condition.height.range", minY, maxY);
    }
}
