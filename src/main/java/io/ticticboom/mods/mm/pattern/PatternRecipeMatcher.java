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
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the recipe an AE2 pattern is for. Everything the pattern carries has to be consumed by the recipe, each
 * resource in the recipe's full amount (a recipe using the same item in several inputs counts their sum); what the
 * pattern leaves out comes from the machine's ports, like what a pattern can't carry (energy, mana...). Catalysts and
 * per-tick inputs may come either way. The pattern's primary output has to be one of the recipe's sure outputs.
 */
public final class PatternRecipeMatcher {
    private PatternRecipeMatcher() {
    }

    /**
     * @return the candidate the pattern is for, or null. When several fit, the one needing the fewest inputs from the
     * ports, then the smallest recipe id.
     */
    @Nullable
    public static RecipeModel match(List<RecipeModel> candidates, PatternSpec spec) {
        RecipeModel found = null;
        int foundFromPorts = 0;
        for (RecipeModel recipe : candidates) {
            if (recipe.mode() != RecipeMode.FIXED || !makesPrimaryOutput(recipe, spec)) {
                continue;
            }
            Fit fit = fit(recipe, spec);
            if (fit.mismatch() == null && (found == null || fit.fromPorts() < foundFromPorts
                    || (fit.fromPorts() == foundFromPorts && recipe.id().toString().compareTo(found.id().toString()) < 0))) {
                found = recipe;
                foundFromPorts = fit.fromPorts();
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
        PatternMismatch inputs = fit(closest, spec).mismatch();
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

    /**
     * How a pattern fits a recipe.
     *
     * @param mismatch  the first difference, null if it fits
     * @param fromPorts how many of the recipe's inputs the pattern leaves to the machine's ports
     */
    private record Fit(@Nullable PatternMismatch mismatch, int fromPorts) {
    }

    private static Fit fit(RecipeModel recipe, PatternSpec spec) {
        var entries = new ArrayList<ConsumeRecipeIngredientEntry>();
        for (IRecipeIngredientEntry entry : recipe.inputs().inputs()) {
            // what a pattern can't carry is left out: the machine's ports provide it
            if (entry instanceof ConsumeRecipeIngredientEntry consume && consume.getIngredient().asPatternIngredient() != null) {
                entries.add(consume);
            }
        }
        var covered = new boolean[entries.size()];
        for (PatternIngredient given : spec.inputs()) {
            // the pattern's inputs are merged per resource, so add up every input of the recipe taking this one
            long needed = 0;
            long optional = 0;
            boolean used = false;
            for (int i = 0; i < entries.size(); i++) {
                ConsumeRecipeIngredientEntry consume = entries.get(i);
                PatternIngredient wanted = consume.getIngredient().asPatternIngredient();
                if (!accepts(consume.getIngredient(), wanted, given)) {
                    continue;
                }
                used = true;
                covered[i] = true;
                if (isOptional(consume)) {
                    optional += wanted.amount();
                } else {
                    needed += wanted.amount();
                }
            }
            if (!used) {
                return new Fit(new PatternMismatch(PatternMismatch.Reason.EXTRA, given.id(), given.amount(), 0), 0);
            }
            // all of what the recipe needs, plus none, some or all of its catalysts; what the run doesn't use goes
            // back to the pattern provider
            if (given.amount() < needed || given.amount() > needed + optional) {
                long recipeAmount = given.amount() < needed ? needed : needed + optional;
                return new Fit(new PatternMismatch(PatternMismatch.Reason.AMOUNT, given.id(), given.amount(), recipeAmount), 0);
            }
        }
        int fromPorts = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (!covered[i] && !isOptional(entries.get(i))) {
                fromPorts++;
            }
        }
        return new Fit(null, fromPorts);
    }

    /** Catalysts and per-tick inputs may come in the pattern (JEI puts them there) or from the ports. */
    private static boolean isOptional(ConsumeRecipeIngredientEntry consume) {
        return consume.getChance() <= 0 || consume.isPerTick();
    }

    /** Items go through the recipe's own item check (components, NBT match); other kinds compare as they are. */
    private static boolean accepts(IPortIngredient ingredient, PatternIngredient wanted, PatternIngredient given) {
        if (wanted.kind() != given.kind() || !wanted.id().equals(given.id())) {
            return false;
        }
        if (ingredient instanceof BaseItemPortIngredient item) {
            var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(given.id()));
            stack.applyComponents(given.components());
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
