package io.ticticboom.mods.mm.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * An AE2 pattern as MM sees it: what one craft takes and the output it's made for. Inputs of the same resource are
 * added together.
 */
public record PatternSpec(List<PatternIngredient> inputs, PatternIngredient primaryOutput) {

    public PatternSpec {
        var merged = new ArrayList<PatternIngredient>();
        for (PatternIngredient input : inputs) {
            int same = -1;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).sameResource(input)) {
                    same = i;
                    break;
                }
            }
            if (same < 0) {
                merged.add(input);
            } else {
                merged.set(same, merged.get(same).withAmount(merged.get(same).amount() + input.amount()));
            }
        }
        inputs = List.copyOf(merged);
    }
}
