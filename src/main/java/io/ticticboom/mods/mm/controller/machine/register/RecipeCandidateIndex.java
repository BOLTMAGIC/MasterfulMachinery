package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structure's recipes, indexed by every exact item they consume, so a controller only looks at recipes whose
 * inputs could be in its ports instead of scanning every recipe of the structure.
 */
public class RecipeCandidateIndex {
    private final List<RecipeModel> recipes;
    private final RecipeRequirements[] requirements;
    // Every exact item input points to the recipes that consume it.
    private final Map<ResourceLocation, int[]> positionsByItem = new HashMap<>();
    // recipes with no specific item input (tags, fluids, energy...), always candidates
    private final int[] unindexedPositions;

    public RecipeCandidateIndex(Collection<RecipeModel> structureRecipes) {
        this.recipes = new ArrayList<>(structureRecipes);
        this.requirements = new RecipeRequirements[recipes.size()];
        Map<ResourceLocation, List<Integer>> byItem = new HashMap<>();
        List<Integer> unindexed = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            var req = RecipeRequirements.of(recipes.get(i));
            requirements[i] = req;
            if (req.itemIds().isEmpty()) {
                unindexed.add(i);
            } else {
                for (ResourceLocation item : req.itemIds()) {
                    byItem.computeIfAbsent(item, k -> new ArrayList<>()).add(i);
                }
            }
        }
        byItem.forEach((item, positions) -> positionsByItem.put(item, toArray(positions)));
        this.unindexedPositions = toArray(unindexed);
    }

    public int size() {
        return recipes.size();
    }

    public boolean isEmpty() {
        return recipes.isEmpty();
    }

    public RecipeModel get(int position) {
        return recipes.get(position);
    }

    /**
     * @return positions, ascending, of the recipes the ports might be able to run
     */
    public int[] candidates(StorageCacheManager.StorageCache cache, @Nullable Set<ResourceLocation> portTypes) {
        BitSet found = new BitSet(recipes.size());
        BitSet tested = new BitSet(recipes.size());
        addMatching(unindexedPositions, cache, portTypes, tested, found);
        for (ResourceLocation item : cache.availableItemIds) {
            int[] positions = positionsByItem.get(item);
            if (positions != null) addMatching(positions, cache, portTypes, tested, found);
        }
        int[] result = new int[found.cardinality()];
        int index = 0;
        for (int position = found.nextSetBit(0); position >= 0; position = found.nextSetBit(position + 1)) {
            result[index++] = position;
        }
        return result;
    }

    private void addMatching(int[] positions, StorageCacheManager.StorageCache cache,
                             @Nullable Set<ResourceLocation> portTypes, BitSet tested, BitSet out) {
        for (int position : positions) {
            if (tested.get(position)) continue;
            tested.set(position);
            var req = requirements[position];
            if (req.hasPortTypes(portTypes) && req.isMetBy(cache)) out.set(position);
        }
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) array[i] = list.get(i);
        return array;
    }
}
