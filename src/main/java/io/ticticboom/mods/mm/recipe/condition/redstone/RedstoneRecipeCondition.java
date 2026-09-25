package io.ticticboom.mods.mm.recipe.condition.redstone;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The controller must (not) get a redstone signal: {"type": "mm:redstone", "redstone": "powered"}.
 * Unlike the controller's redstone setting this is part of the recipe, so it can differ per recipe.
 */
public class RedstoneRecipeCondition implements IRecipeCondition {
    private final boolean powered;

    public RedstoneRecipeCondition(boolean powered) {
        this.powered = powered;
    }

    public static IRecipeCondition parse(JsonObject json) {
        return new RedstoneRecipeCondition(!"unpowered".equals(json.get("redstone").getAsString().toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.pos() == null) return true;
        return ctx.level().hasNeighborSignal(ctx.pos()) == powered;
    }

    @Override
    public Component describe() {
        return Component.translatable(powered ? "jei.mm.condition.redstone.powered" : "jei.mm.condition.redstone.unpowered");
    }
}
