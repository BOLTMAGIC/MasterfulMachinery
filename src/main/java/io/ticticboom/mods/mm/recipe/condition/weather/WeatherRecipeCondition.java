package io.ticticboom.mods.mm.recipe.condition.weather;

import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import net.minecraft.world.level.Level;

public class WeatherRecipeCondition implements IRecipeCondition {

    private final RecipeWeatherType type;

    public WeatherRecipeCondition(RecipeWeatherType type) {
        this.type = type;
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        var level = ctx.level();
        return switch (type) {
            case RAIN -> level.isRaining();
            case THUNDER -> level.isThundering();
            case CLEAR -> !level.isThundering() && !level.isRaining();
        };
    }

    @Override
    public Component describe() {
        return Component.translatable("jei.mm.condition.weather." + type.name().toLowerCase(Locale.ROOT));
    }
}
