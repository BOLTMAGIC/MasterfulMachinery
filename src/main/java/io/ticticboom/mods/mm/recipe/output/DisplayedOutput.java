package io.ticticboom.mods.mm.recipe.output;

import io.ticticboom.mods.mm.port.IPortIngredient;

/**
 * One thing a recipe output entry can produce, for the controller screen.
 *
 * @param chance 0-1, how likely it is produced each time the recipe finishes
 */
public record DisplayedOutput(IPortIngredient ingredient, double chance) {
}
