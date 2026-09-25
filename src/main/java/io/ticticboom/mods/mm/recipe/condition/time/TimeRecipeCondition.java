package io.ticticboom.mods.mm.recipe.condition.time;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Only by day or only at night: {"type": "mm:time", "time": "night"}.
 */
public class TimeRecipeCondition implements IRecipeCondition {
    private final boolean day;

    public TimeRecipeCondition(boolean day) {
        this.day = day;
    }

    public static IRecipeCondition parse(JsonObject json) {
        return new TimeRecipeCondition(!"night".equals(json.get("time").getAsString().toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        // the sun is up for world time 0..12999 of each day
        long time = ctx.level().getDayTime() % 24000L;
        boolean isDay = time < 13000L;
        return isDay == day;
    }

    @Override
    public Component describe() {
        return Component.translatable(day ? "jei.mm.condition.time.day" : "jei.mm.condition.time.night");
    }
}
