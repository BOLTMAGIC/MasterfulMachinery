package io.ticticboom.mods.mm.port.item;

import net.minecraft.nbt.CompoundTag;
import java.util.IdentityHashMap;

/**
 * Cache for CompoundTag hashes to optimize performance.
 * NBT tag comparisons are expensive, so we cache hashes
 * to enable fast inequality detection.
 */
public class CompoundTagCache {

    // IdentityHashMap for fast reference-based lookups
    private static final IdentityHashMap<CompoundTag, Integer> TAG_HASH_CACHE = new IdentityHashMap<>(256);

    /**
     * Returns a fast hash for a CompoundTag.
     * The hash is cached and consistent for the same instance.
     */
    public static int getTagHash(CompoundTag tag) {
        if (tag == null) return 0;

        // Fast lookup in cache (based on object identity)
        Integer cached = TAG_HASH_CACHE.get(tag);
        if (cached != null) {
            return cached;
        }

        // Calculate tag hash (simple but fast)
        int hash = tag.getAllKeys().size() * 31;
        for (String key : tag.getAllKeys()) {
            hash = hash * 31 + key.hashCode();
        }

        // Limit cache size to save memory
        if (TAG_HASH_CACHE.size() >= 512) {
            TAG_HASH_CACHE.clear();
        }

        TAG_HASH_CACHE.put(tag, hash);
        return hash;
    }

    /**
     * Compares two NBT tags quickly.
     * Uses hashes for fast inequality detection.
     */
    public static boolean areTagsDifferent(CompoundTag a, CompoundTag b) {
        // Null checks
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;

        // Reference comparison (fast if same instance)
        if (a == b) return false;

        // Hash comparison (fast if sizes differ)
        int hashA = getTagHash(a);
        int hashB = getTagHash(b);
        if (hashA != hashB) return true;

        // Fallback: check actual equality
        return !a.equals(b);
    }

    /**
     * Clears the cache (e.g., after major changes).
     */
    public static void clearCache() {
        TAG_HASH_CACHE.clear();
    }
}


