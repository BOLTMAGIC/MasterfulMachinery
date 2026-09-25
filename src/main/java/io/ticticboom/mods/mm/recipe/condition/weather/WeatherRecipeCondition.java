package io.ticticboom.mods.mm.recipe.condition.weather;

import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class WeatherRecipeCondition implements IRecipeCondition {

    private final RecipeWeatherType type;

    public WeatherRecipeCondition(RecipeWeatherType type) {
        this.type = type;
    }

    @Override
    public Component describe() {
        return Component.translatable("jei.mm.condition.weather." + type.name().toLowerCase());
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        Level level = ctx.level();
        if (type == RecipeWeatherType.RAIN) {
            return level.isRaining();
        } else if (type == RecipeWeatherType.THUNDER) {
            return level.isThundering();
        } else if (type == RecipeWeatherType.CLEAR) {
            return !level.isThundering() && !level.isRaining();
        }
        return false;
    }
}
