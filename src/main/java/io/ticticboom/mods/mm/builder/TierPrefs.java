package io.ticticboom.mods.mm.builder;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Preferred tier per port type and direction, keyed like "mm:item/input" (see {@link PortTiers#key}).
 * Missing keys mean {@link TierResolver#LOWEST}.
 */
public final class TierPrefs {
    private final Map<String, Integer> prefs = new HashMap<>();

    public int get(String key) {
        return prefs.getOrDefault(key, TierResolver.LOWEST);
    }

    public void set(String key, int rank) {
        if (rank <= TierResolver.LOWEST) {
            prefs.remove(key);
        } else {
            prefs.put(key, rank);
        }
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
            result.set(key, tag.getInt(key));
        }
        return result;
    }
}
