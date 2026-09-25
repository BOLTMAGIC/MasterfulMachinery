package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structure's recipes, indexed by one item each consumes, so a controller only looks at recipes whose
 * inputs could be in its ports instead of scanning every recipe of the structure.
 */
public class RecipeCandidateIndex {
    private final List<RecipeModel> recipes;
    private final RecipeRequirements[] requirements;
    // position of every recipe that consumes a specific item, under the first such item
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
                byItem.computeIfAbsent(req.itemIds().iterator().next(), k -> new ArrayList<>()).add(i);
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
        List<Integer> found = new ArrayList<>();
        addMatching(unindexedPositions, cache, portTypes, found);
        for (ResourceLocation item : cache.availableItemIds) {
            int[] positions = positionsByItem.get(item);
            if (positions != null) addMatching(positions, cache, portTypes, found);
        }
        int[] result = toArray(found);
        Arrays.sort(result);
        return result;
    }

    private void addMatching(int[] positions, StorageCacheManager.StorageCache cache,
                             @Nullable Set<ResourceLocation> portTypes, List<Integer> out) {
        for (int position : positions) {
            var req = requirements[position];
            if (req.hasPortTypes(portTypes) && req.isMetBy(cache)) out.add(position);
        }
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) array[i] = list.get(i);
        return array;
    }
}
