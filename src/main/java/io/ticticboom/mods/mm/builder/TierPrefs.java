package io.ticticboom.mods.mm.builder;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preferred tier per port type and direction, keyed like "mm:item/input" (see {@link PortTiers#key}).
 * Missing keys mean {@link TierResolver#LOWEST}.
 */
public final class TierPrefs {
    /** Most entries kept from saved data; a machine has only a handful of port types. */
    public static final int MAX_ENTRIES = 32;
    /** Returned by {@link #validate} for a key the machine does not have. */
    public static final int REJECTED = -1;

    private final Map<String, Integer> prefs = new HashMap<>();

    public int get(String key) {
        return prefs.getOrDefault(key, TierResolver.LOWEST);
    }

    /** @return true when the stored value changed */
    public boolean set(String key, int rank) {
        if (rank <= TierResolver.LOWEST) {
            return prefs.remove(key) != null;
        }
        Integer old = prefs.put(key, rank);
        return old == null || old != rank;
    }

    /**
     * Checks a tier a client asked for.
     *
     * @param maxTierByKey every key the machine's structures have, with the highest tier they offer for it
     * @return the tier to store, clamped to [{@link TierResolver#LOWEST}, max], or {@link #REJECTED} for an unknown key
     */
    public static int validate(Map<String, Integer> maxTierByKey, String key, int rank) {
        Integer max = maxTierByKey.get(key);
        if (max == null) {
            return REJECTED;
        }
        return Math.max(TierResolver.LOWEST, Math.min(max, rank));
    }

    /**
     * The next choice when a player steps through "lowest" followed by {@code tiers} (ascending), wrapping around;
     * an unknown current value counts as "lowest".
     */
    public static int cycle(int current, Collection<Integer> tiers, int step) {
        List<Integer> options = new ArrayList<>();
        options.add(TierResolver.LOWEST);
        options.addAll(tiers);
        int index = Math.max(0, options.indexOf(current));
        return options.get(Math.floorMod(index + step, options.size()));
    }

    public Map<String, Integer> asMap() {
        return Collections.unmodifiableMap(prefs);
    }

    public void copyFrom(TierPrefs other) {
        prefs.clear();
        prefs.putAll(other.prefs);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        prefs.forEach(tag::putInt);
        return tag;
    }

    public static TierPrefs load(CompoundTag tag) {
        var result = new TierPrefs();
        for (String key : tag.getAllKeys()) {
            if (result.prefs.size() >= MAX_ENTRIES) {
                break;
            }
            result.set(key, tag.getInt(key));
        }
        return result;
    }
}
