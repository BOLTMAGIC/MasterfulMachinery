package io.ticticboom.mods.mm.pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Why a pattern fits none of the machine's recipes, compared with the closest recipe (the first one making the
 * pattern's main output), for Jade.
 *
 * @param what           the resource it is about
 * @param patternAmount  how much of it the pattern has (0: none)
 * @param recipeAmount   how much of it the recipe uses or makes (0: none)
 */
public record PatternMismatch(Reason reason, Identifier what, long patternAmount, long recipeAmount) {

    public enum Reason {
        /** No recipe of the machine makes the pattern's main output. */
        NO_OUTPUT,
        /** The recipe makes less of the main output than the pattern expects. */
        OUTPUT_AMOUNT,
        /** The pattern has the input, in another amount. */
        AMOUNT,
        /** The pattern has something the recipe doesn't use. */
        EXTRA,
        /** The pattern has something MM can't carry (energy, mana... through an AE2 addon): those come from the ports. */
        UNSUPPORTED
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Reason", reason.name().toLowerCase(Locale.ROOT));
        tag.putString("What", what.toString());
        tag.putLong("Pattern", patternAmount);
        tag.putLong("Recipe", recipeAmount);
        return tag;
    }

    @Nullable
    public static PatternMismatch load(CompoundTag tag) {
        Identifier what = Identifier.tryParse(tag.getString("What"));
        if (what == null) {
            return null;
        }
        try {
            return new PatternMismatch(Reason.valueOf(tag.getString("Reason").toUpperCase(Locale.ROOT)), what,
                    tag.getLong("Pattern"), tag.getLong("Recipe"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
