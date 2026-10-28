package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import net.minecraft.resources.Identifier;

public record EntityIngredient(
        Identifier id,
        boolean tag,
        int amount
) {
}
