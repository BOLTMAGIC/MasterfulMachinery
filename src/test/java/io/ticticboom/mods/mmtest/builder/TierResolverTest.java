package io.ticticboom.mods.mmtest.builder;

import io.ticticboom.mods.mm.builder.TierResolver;

import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierResolverTest {

    private static SortedSet<Integer> tiers(Integer... ranks) {
        return new TreeSet<>(java.util.List.of(ranks));
    }

    @Test
    void lowestPreferenceTakesSmallestAllowed() {
        assertEquals(2, TierResolver.resolve(TierResolver.LOWEST, 2, 5, tiers(1, 2, 3, 5)));
    }

    @Test
    void preferredInsideRangeIsKept() {
        assertEquals(3, TierResolver.resolve(3, 1, 5, tiers(1, 2, 3, 5)));
    }

    @Test
    void preferredBelowRangeClampsUp() {
        // a machine that only takes the biggest energy port
        assertEquals(5, TierResolver.resolve(1, 5, 5, tiers(1, 2, 3, 5)));
    }

    @Test
    void preferredAboveRangeClampsDown() {
        assertEquals(2, TierResolver.resolve(4, 1, 2, tiers(1, 2, 3, 5)));
    }

    @Test
    void gapPicksNearestExistingTier() {
        // T4 does not exist: 3 and 5 are both one away -> the smaller one
        assertEquals(3, TierResolver.resolve(4, 1, 5, tiers(1, 3, 5)));
        // T2 does not exist, 1 is closer than 5
        assertEquals(1, TierResolver.resolve(2, 1, 5, tiers(1, 5)));
    }

    @Test
    void unboundedMaxIsAccepted() {
        assertEquals(5, TierResolver.resolve(9, 1, Integer.MAX_VALUE, tiers(1, 2, 5)));
    }

    @Test
    void nothingAllowedReturnsMinusOne() {
        assertEquals(-1, TierResolver.resolve(1, 4, 4, tiers(1, 2, 3)));
        assertEquals(-1, TierResolver.resolve(1, 1, 5, tiers()));
    }

    @Test
    void invertedRangeFitsNothing() {
        assertEquals(-1, TierResolver.resolve(2, 3, 1, tiers(1, 2, 3)));
    }
}
