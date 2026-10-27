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
        // AEKey.getId() is the chemical's registry id
        return new PatternIngredient(Kind.CHEMICAL, chemical.getId(), amount);
    }
}
