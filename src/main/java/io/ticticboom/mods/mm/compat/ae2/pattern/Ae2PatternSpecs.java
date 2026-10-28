package io.ticticboom.mods.mm.compat.ae2.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.pattern.PatternMismatch;
import io.ticticboom.mods.mm.pattern.PatternSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/** AE2 patterns and stacks as MM sees them. */
public final class Ae2PatternSpecs {
    private static final boolean APPMEK = ModList.get().isLoaded("appmek");

    private Ae2PatternSpecs() {
    }

    /** @return null if the pattern has something MM can't carry */
    @Nullable
    public static PatternSpec spec(IPatternDetails details) {
        var inputs = new ArrayList<PatternIngredient>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                return null;
            }
            PatternIngredient ingredient = ingredient(possible[0].what(), possible[0].amount() * input.getMultiplier());
            if (ingredient == null) {
                return null;
            }
            inputs.add(ingredient);
        }
        GenericStack primary = details.getPrimaryOutput();
        PatternIngredient output = primary == null ? null : ingredient(primary.what(), primary.amount());
        return output == null ? null : new PatternSpec(inputs, output);
    }

    /** @return the first of the pattern's inputs or main output MM can't carry, or null */
    @Nullable
    public static PatternMismatch unsupported(IPatternDetails details) {
        for (IPatternDetails.IInput input : details.getInputs()) {
            for (GenericStack possible : input.getPossibleInputs()) {
                if (ingredient(possible.what(), possible.amount()) == null) {
                    return new PatternMismatch(PatternMismatch.Reason.UNSUPPORTED, possible.what().getId(), possible.amount() * input.getMultiplier(), 0);
                }
            }
        }
        GenericStack primary = details.getPrimaryOutput();
        if (primary != null && ingredient(primary.what(), primary.amount()) == null) {
            return new PatternMismatch(PatternMismatch.Reason.UNSUPPORTED, primary.what().getId(), primary.amount(), 0);
        }
        return null;
    }

    /** @return null for a kind of key MM can't carry */
    @Nullable
    public static PatternIngredient ingredient(AEKey key, long amount) {
        if (key instanceof AEItemKey item) {
            return new PatternIngredient(Kind.ITEM, BuiltInRegistries.ITEM.getKey(item.getItem()), amount,
                    item.toStack().getComponentsPatch());
        }
        if (key instanceof AEFluidKey fluid) {
            return new PatternIngredient(Kind.FLUID, BuiltInRegistries.FLUID.getKey(fluid.getFluid()), amount);
        }
        return null; // [26.1-pending] Applied Mekanistics: APPMEK ? Ae2ChemicalKeys.ingredient(key, amount) : null
    }
}
