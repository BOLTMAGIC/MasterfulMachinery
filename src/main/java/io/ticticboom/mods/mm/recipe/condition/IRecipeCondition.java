package io.ticticboom.mods.mm.recipe.condition;

import net.minecraft.network.chat.Component;

public interface IRecipeCondition {
    boolean canRun(RecipeConditionContext ctx);

    Component describe();
}
