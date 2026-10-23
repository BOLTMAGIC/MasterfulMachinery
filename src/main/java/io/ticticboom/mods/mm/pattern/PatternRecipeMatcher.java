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
 * chemicals, with their amounts); everything a pattern can't carry (energy, catalysts that aren't consumed, per-tick
 * inputs...) comes from the machine's ports. The pattern's primary output has to be one of the recipe's sure outputs.
 */
public final class PatternRecipeMatcher {
    private PatternRecipeMatcher() {
    }

    /** @return the candidate the pattern is for (the smallest recipe id when several fit), or null */
    @Nullable
    public static RecipeModel match(List<RecipeModel> candidates, PatternSpec spec) {
        RecipeModel found = null;
        for (RecipeModel recipe : candidates) {
            if (recipe.mode() == RecipeMode.FIXED && inputsMatch(recipe, spec) && makesPrimaryOutput(recipe, spec)
                    && (found == null || recipe.id().toString().compareTo(found.id().toString()) < 0)) {
                found = recipe;
            }
        }
        return found;
    }

    private static boolean inputsMatch(RecipeModel recipe, PatternSpec spec) {
        var left = new ArrayList<>(spec.inputs());
        for (IRecipeIngredientEntry entry : recipe.inputs().inputs()) {
            if (!(entry instanceof ConsumeRecipeIngredientEntry consume) || consume.getChance() <= 0 || consume.isPerTick()) {
                continue;
            }
            IPortIngredient ingredient = consume.getIngredient();
            PatternIngredient wanted = ingredient.asPatternIngredient();
            if (wanted == null) {
                // not something a pattern carries: the machine's ports provide it
                continue;
            }
            int found = -1;
            for (int i = 0; i < left.size(); i++) {
                if (left.get(i).amount() == wanted.amount() && accepts(ingredient, wanted, left.get(i))) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return false;
            }
            left.remove(found);
        }
        return left.isEmpty();
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
        PatternIngredient primary = spec.primaryOutput();
        for (IRecipeOutputEntry entry : recipe.outputs().outputs()) {
            if (!(entry instanceof SimpleRecipeOutputEntry simple) || simple.getChance() < 1 || simple.isPerTick()) {
                continue;
            }
            PatternIngredient made = simple.getIngredient().asPatternIngredient();
            if (made != null && made.sameResource(primary) && made.amount() >= primary.amount()) {
                return true;
            }
        }
        return false;
    }
}
