package io.ticticboom.mods.mm.builder;

import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Picks which port tier goes into a structure position that accepts a range of tiers.
 * The player's preference is clamped into the range, then the nearest tier that really
 * exists is used (a pack may register T1, T2 and T5 only).
 */
public final class TierResolver {
    /** "No preference": use the smallest tier the position accepts. */
    public static final int LOWEST = 0;

    private TierResolver() {
    }

    /**
     * @return the chosen tier, or -1 when no existing tier fits the range
     */
    public static int resolve(int preferred, int minTier, int maxTier, SortedSet<Integer> available) {
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
        // on a tie the smaller (cheaper) tier wins
        return target - below <= above - target ? below : above;
    }
}
