package io.ticticboom.mods.mm.compat.ae2.pattern;

import appeng.api.stacks.AEKey;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import org.jetbrains.annotations.Nullable;

/** Mekanism chemicals in AE2 patterns (Applied Mekanistics); only touched with Applied Mekanistics installed. */
final class Ae2ChemicalKeys {
    private Ae2ChemicalKeys() {
    }

    @Nullable
    static PatternIngredient ingredient(AEKey key, long amount) {
        if (!(key instanceof MekanismKey chemical)) {
            return null;
        }
        Kind kind = switch (chemical.getForm()) {
            case MekanismKey.GAS -> Kind.GAS;
            case MekanismKey.INFUSION -> Kind.INFUSION;
            case MekanismKey.PIGMENT -> Kind.PIGMENT;
            case MekanismKey.SLURRY -> Kind.SLURRY;
            default -> null;
        };
        return kind == null ? null : new PatternIngredient(kind, chemical.getStack().getTypeRegistryName(), amount, null);
    }
}
