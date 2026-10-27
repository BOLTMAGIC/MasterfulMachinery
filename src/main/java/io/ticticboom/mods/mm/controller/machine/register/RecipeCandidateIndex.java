package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structure's recipes, indexed by every exact item they consume, so a controller only looks at recipes whose
 * inputs could be in its ports instead of scanning every recipe of the structure.
 */
class RecipeCandidateIndex {
    private final RecipeRequirements[] requirements;
    // Every exact item input points to the recipes that consume it.
    private final Map<ResourceLocation, int[]> positionsByItem = new HashMap<>();
    // recipes with no specific item input (tags, fluids, energy...), checked whatever items the ports hold
    private final int[] unindexedPositions;
    private final int[] allPositions;

    RecipeCandidateIndex(List<RecipeModel> structureRecipes) {
        requirements = new RecipeRequirements[structureRecipes.size()];
        allPositions = new int[structureRecipes.size()];
        Map<ResourceLocation, List<Integer>> byItem = new HashMap<>();
        List<Integer> unindexed = new ArrayList<>();
        for (int i = 0; i < structureRecipes.size(); i++) {
            var req = RecipeRequirements.of(structureRecipes.get(i));
            requirements[i] = req;
            allPositions[i] = i;
            if (req.itemIds().isEmpty()) {
                unindexed.add(i);
            } else {
                for (ResourceLocation item : req.itemIds()) {
                    byItem.computeIfAbsent(item, k -> new ArrayList<>()).add(i);
                }
            }
        }
        byItem.forEach((item, positions) -> positionsByItem.put(item, toArray(positions)));
        unindexedPositions = toArray(unindexed);
    }

    RecipeRequirements requirements(int position) {
        return requirements[position];
    }

    /** Every position, for when the port contents aren't known. */
    int[] all() {
        return allPositions;
    }

    /**
     * @return positions, ascending, of the recipes whose item, fluid and energy inputs the ports might hold
     */
    int[] candidates(Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean hasEnergy) {
        BitSet found = new BitSet(requirements.length);
        BitSet tested = new BitSet(requirements.length);
        addMatching(unindexedPositions, itemIds, fluidIds, hasEnergy, tested, found);
        for (ResourceLocation item : itemIds) {
            int[] positions = positionsByItem.get(item);
            if (positions != null) addMatching(positions, itemIds, fluidIds, hasEnergy, tested, found);
        }
        int[] result = new int[found.cardinality()];
        int index = 0;
        for (int position = found.nextSetBit(0); position >= 0; position = found.nextSetBit(position + 1)) {
            result[index++] = position;
        }
        return result;
    }

    private void addMatching(int[] positions, Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds,
                             boolean hasEnergy, BitSet tested, BitSet out) {
        for (int position : positions) {
            if (tested.get(position)) continue;
            tested.set(position);
            var req = requirements[position];
            if (itemIds.containsAll(req.itemIds()) && fluidIds.containsAll(req.fluidIds()) && (!req.energy() || hasEnergy)) {
                out.set(position);
            }
        }
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) array[i] = list.get(i);
        return array;
    }
}
