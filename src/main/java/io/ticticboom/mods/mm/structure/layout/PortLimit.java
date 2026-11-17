package io.ticticboom.mods.mm.structure.layout;

/**
 * How many ports of one port key a structure with port slots takes.
 *
 * @param positions how many positions the layout draws of the key
 * @param min       the fewest ports of the key the structure forms with
 * @param max       the most; {@link Integer#MAX_VALUE} for no limit
 */
public record PortLimit(int positions, int min, int max) {
    public boolean unlimited() {
        return max == Integer.MAX_VALUE;
    }

    /** Whether the count differs from simply one port per drawn position. */
    public boolean ranged() {
        return min != positions || max != positions;
    }
}
