package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import net.minecraft.resources.Identifier;

public record MatterIngredient(
        Identifier matter,
        int amount,
        int color
) {
}
