package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.flex.RecipeMode;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the recipe an AE2 pattern is for. The pattern carries exactly what the recipe consumes (items, fluids,
 * chemicals, with their amounts); what a pattern can't carry (energy, mana...) comes from the machine's ports, and
 * catalysts and per-tick inputs may come either way. The pattern's primary output has to be one of the recipe's sure
 * outputs.
 */
public final class PatternRecipeMatcher {
    private PatternRecipeMatcher() {
    }

    /** @return the candidate the pattern is for (the smallest recipe id when several fit), or null */
    @Nullable
    public static RecipeModel match(List<RecipeModel> candidates, PatternSpec spec) {
        RecipeModel found = null;
        for (RecipeModel recipe : candidates) {
            if (recipe.mode() == RecipeMode.FIXED && inputMismatch(recipe, spec) == null && makesPrimaryOutput(recipe, spec)
                    && (found == null || recipe.id().toString().compareTo(found.id().toString()) < 0)) {
                found = recipe;
            }
        }
        return found;
    }

    /**
     * Why the pattern fits none of the candidates, against the closest one: of the recipes making the pattern's main
     * output, the one consuming the most of the pattern's resources (then the smallest id). Null if a candidate fits.
     */
    @Nullable
    public static PatternMismatch explain(List<RecipeModel> candidates, PatternSpec spec) {
        if (match(candidates, spec) != null) {
            return null;
        }
        PatternIngredient primary = spec.primaryOutput();
        RecipeModel closest = null;
        long made = 0;
        int closestShared = -1;
        for (RecipeModel recipe : candidates) {
            long amount = sureAmountOf(recipe, primary);
            if (recipe.mode() != RecipeMode.FIXED || amount <= 0) {
                continue;
            }
            int shared = sharedResources(recipe, spec);
            if (shared > closestShared || (shared == closestShared && recipe.id().toString().compareTo(closest.id().toString()) < 0)) {
                closest = recipe;
                made = amount;
                closestShared = shared;
            }
        }
        if (closest == null) {
            return new PatternMismatch(PatternMismatch.Reason.NO_OUTPUT, primary.id(), primary.amount(), 0);
        }
        PatternMismatch inputs = inputMismatch(closest, spec);
        if (inputs != null) {
            return inputs;
        }
        return new PatternMismatch(PatternMismatch.Reason.OUTPUT_AMOUNT, primary.id(), primary.amount(), made);
    }

    /** How many of the pattern's inputs the recipe consumes, whatever the amount. */
    private static int sharedResources(RecipeModel recipe, PatternSpec spec) {
        int shared = 0;
        for (PatternIngredient given : spec.inputs()) {
            for (IRecipeIngredientEntry entry : recipe.inputs().inputs()) {
                PatternIngredient wanted = entry instanceof ConsumeRecipeIngredientEntry consume ? consume.getIngredient().asPatternIngredient() : null;
                if (wanted != null && wanted.kind() == given.kind() && wanted.id().equals(given.id())) {
                    shared++;
                    break;
                }
            }
        }
        return shared;
    }

    /** @return null if the pattern carries exactly what the recipe consumes, else the first difference */
    @Nullable
    private static PatternMismatch inputMismatch(RecipeModel recipe, PatternSpec spec) {
        var left = new ArrayList<>(spec.inputs());
        for (IRecipeIngredientEntry entry : recipe.inputs().inputs()) {
            if (!(entry instanceof ConsumeRecipeIngredientEntry consume)) {
                continue;
            }
            IPortIngredient ingredient = consume.getIngredient();
            PatternIngredient wanted = ingredient.asPatternIngredient();
            if (wanted == null) {
                // not something a pattern carries: the machine's ports provide it
                continue;
            }
            // catalysts and per-tick inputs may come in the pattern (JEI puts them there) or from the ports; what the
            // run doesn't use goes back to the pattern provider
            boolean optional = consume.getChance() <= 0 || consume.isPerTick();
            int found = -1;
            for (int i = 0; i < left.size(); i++) {
                if (left.get(i).amount() == wanted.amount() && accepts(ingredient, wanted, left.get(i))) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                left.remove(found);
            } else if (!optional) {
                for (PatternIngredient given : left) {
                    if (given.kind() == wanted.kind() && given.id().equals(wanted.id())) {
                        return new PatternMismatch(PatternMismatch.Reason.AMOUNT, wanted.id(), given.amount(), wanted.amount());
                    }
                }
                return new PatternMismatch(PatternMismatch.Reason.MISSING, wanted.id(), 0, wanted.amount());
            }
        }
        return left.isEmpty() ? null : new PatternMismatch(PatternMismatch.Reason.EXTRA, left.get(0).id(), left.get(0).amount(), 0);
    }

    /** Items go through the recipe's own item check (exact or partial NBT match); other kinds compare as they are. */
    private static boolean accepts(IPortIngredient ingredient, PatternIngredient wanted, PatternIngredient given) {
        if (wanted.kind() != given.kind() || !wanted.id().equals(given.id())) {
            return false;
        }
        if (ingredient instanceof BaseItemPortIngredient item) {
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(given.id()));
            stack.setTag(given.nbt() == null ? null : given.nbt().copy());
            return item.matches(stack);
        }
        return wanted.sameResource(given);
    }

    private static boolean makesPrimaryOutput(RecipeModel recipe, PatternSpec spec) {
        return sureAmountOf(recipe, spec.primaryOutput()) >= spec.primaryOutput().amount();
    }

    /** How much of the resource one of the recipe's sure outputs makes (0: none does). */
    private static long sureAmountOf(RecipeModel recipe, PatternIngredient resource) {
        long most = 0;
        for (IRecipeOutputEntry entry : recipe.outputs().outputs()) {
            if (!(entry instanceof SimpleRecipeOutputEntry simple) || simple.getChance() < 1 || simple.isPerTick()) {
                continue;
            }
            PatternIngredient made = simple.getIngredient().asPatternIngredient();
            if (made != null && made.sameResource(resource)) {
                most = Math.max(most, made.amount());
            }
        }
        return most;
    }
}
