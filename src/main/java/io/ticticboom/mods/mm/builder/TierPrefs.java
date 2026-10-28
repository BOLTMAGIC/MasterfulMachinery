package io.ticticboom.mods.mm.builder;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TierPrefs {
    public static final int MAX_ENTRIES = 32;
    public static final int REJECTED = -1;

    private final Map<String, Integer> prefs = new HashMap<>();

    public int get(String key) {
        return prefs.getOrDefault(key, TierResolver.LOWEST);
    }

    public boolean set(String key, int rank) {
        if (rank <= TierResolver.LOWEST) {
            return prefs.remove(key) != null;
        }
        Integer old = prefs.put(key, rank);
        return old == null || old != rank;
    }

    public static int validate(Map<String, Integer> maxTierByKey, String key, int rank) {
        Integer max = maxTierByKey.get(key);
        if (max == null) {
            return REJECTED;
        }
        return Math.max(TierResolver.LOWEST, Math.min(max, rank));
    }

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
        for (String key : tag.keySet()) {
            if (result.prefs.size() >= MAX_ENTRIES) {
                break;
            }
            result.set(key, tag.getIntOr(key, 0));
        }
        return result;
    }
}
