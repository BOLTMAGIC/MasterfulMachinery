package io.ticticboom.mods.mm.recipe.condition;

import net.minecraft.network.chat.Component;

public interface IRecipeCondition {
    boolean canRun(RecipeConditionContext ctx);

    /** One line for JEI, e.g. "Only at night". */
    Component describe();
}
