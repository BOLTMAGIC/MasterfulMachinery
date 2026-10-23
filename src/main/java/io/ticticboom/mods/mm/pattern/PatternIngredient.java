package io.ticticboom.mods.mm.pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One input or output of an AE2 pattern as MM sees it, without any AE2 type: what kind of resource, which one, how
 * much, and an item's NBT.
 */
public record PatternIngredient(Kind kind, ResourceLocation id, long amount, @Nullable CompoundTag nbt) {

    public enum Kind { ITEM, FLUID, GAS, INFUSION, PIGMENT, SLURRY }

    /** Same kind, id and NBT; the amount may differ. */
    public boolean sameResource(PatternIngredient other) {
        return kind == other.kind && id.equals(other.id) && Objects.equals(nbt, other.nbt);
    }

    public PatternIngredient withAmount(long amount) {
        return new PatternIngredient(kind, id, amount, nbt);
    }
}
