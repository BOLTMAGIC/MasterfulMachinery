package io.ticticboom.mods.mm.builder;

import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

public final class TierResolver {
    public static final int LOWEST = 0;

    private TierResolver() {
    }

    public static int resolve(int preferred, int minTier, int maxTier, SortedSet<Integer> available) {
        if (minTier > maxTier) {
            return -1;
        }
        NavigableSet<Integer> allowed = new TreeSet<>(available).subSet(minTier, true, maxTier, true);
        if (allowed.isEmpty()) {
            return -1;
        }
        if (preferred <= LOWEST) {
            return allowed.first();
        }
        int target = Math.max(minTier, Math.min(maxTier, preferred));
        Integer below = allowed.floor(target);
        Integer above = allowed.ceiling(target);
        if (below == null) return above;
        if (above == null) return below;
        return target - below <= above - target ? below : above;
    }
}
