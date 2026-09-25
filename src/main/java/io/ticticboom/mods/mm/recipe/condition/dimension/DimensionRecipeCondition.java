package io.ticticboom.mods.mm.recipe.condition.dimension;

import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DimensionRecipeCondition implements IRecipeCondition {

    private final ResourceLocation id;

    public DimensionRecipeCondition(ResourceLocation id) {
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
