package io.ticticboom.mods.mm.pattern;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;

/**
 * One input or output of an AE2 pattern as MM sees it, without any AE2 type: what kind of resource, which one, how
 * much, and an item's components.
 */
public record PatternIngredient(Kind kind, Identifier id, long amount, DataComponentPatch components) {

    /** Mekanism 1.21 has one kind of chemical (gases, infusions, pigments and slurries are all chemicals). */
    public enum Kind { ITEM, FLUID, CHEMICAL }

    public PatternIngredient {
        components = components == null ? DataComponentPatch.EMPTY : components;
    }

    public PatternIngredient(Kind kind, Identifier id, long amount) {
        this(kind, id, amount, DataComponentPatch.EMPTY);
    }

    /** Same kind, id and components; the amount may differ. */
    public boolean sameResource(PatternIngredient other) {
        return kind == other.kind && id.equals(other.id) && components.equals(other.components);
    }

    public PatternIngredient withAmount(long amount) {
        return new PatternIngredient(kind, id, amount, components);
    }
}
