package io.ticticboom.mods.mm.recipe.condition.dimension;

import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class DimensionRecipeCondition implements IRecipeCondition {

    private final Identifier id;

    public DimensionRecipeCondition(Identifier id) {
        this.id = id;
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        return ctx.level().dimension().location().equals(id);
    }

    @Override
    public Component describe() {
        return Component.translatable("jei.mm.condition.dimension", id.toString());
    }
}
