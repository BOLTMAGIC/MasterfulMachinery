package io.ticticboom.mods.mm.recipe.condition.time;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

import java.util.Locale;

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
        long time = ctx.level().getOverworldClockTime() % 24000L;
        return (time < 13000L) == day;
    }

    @Override
    public Component describe() {
        return Component.translatable(day ? "jei.mm.condition.time.day" : "jei.mm.condition.time.night");
    }
}
