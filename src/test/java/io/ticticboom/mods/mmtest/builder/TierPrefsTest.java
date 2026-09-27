package io.ticticboom.mods.mmtest.builder;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.TierResolver;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierPrefsTest {

    @Test
    void unsetKeyIsLowest() {
        assertEquals(TierResolver.LOWEST, new TierPrefs().get("mm:item/input"));
    }

    @Test
    void setAndGet() {
        var prefs = new TierPrefs();
        prefs.set("mm:item/input", 2);
        assertEquals(2, prefs.get("mm:item/input"));
        assertEquals(TierResolver.LOWEST, prefs.get("mm:item/output"));
    }

    @Test
    void settingLowestRemovesTheEntry() {
        var prefs = new TierPrefs();
        prefs.set("mm:item/input", 3);
        prefs.set("mm:item/input", TierResolver.LOWEST);
        assertTrue(prefs.asMap().isEmpty());
    }

    @Test
    void copyFromReplacesEverything() {
        var a = new TierPrefs();
        a.set("x", 2);
        var b = new TierPrefs();
        b.set("y", 4);
        b.copyFrom(a);
        assertEquals(2, b.get("x"));
        assertEquals(TierResolver.LOWEST, b.get("y"));
    }

    @Test
    void setReportsChanges() {
        var prefs = new TierPrefs();
        assertTrue(prefs.set("mm:item/input", 2));
        assertFalse(prefs.set("mm:item/input", 2));
        assertTrue(prefs.set("mm:item/input", 3));
        assertTrue(prefs.set("mm:item/input", TierResolver.LOWEST));
        assertFalse(prefs.set("mm:item/input", TierResolver.LOWEST));
    }

    @Test
    void validateRejectsUnknownKeys() {
        var max = Map.of("mm:item/input", 3);
        assertEquals(TierPrefs.REJECTED, TierPrefs.validate(max, "mm:item/output", 2));
        assertEquals(TierPrefs.REJECTED, TierPrefs.validate(max, "x".repeat(256), 2));
    }

    @Test
    void validateClampsToExistingTiers() {
        var max = Map.of("mm:item/input", 3);
        assertEquals(2, TierPrefs.validate(max, "mm:item/input", 2));
        assertEquals(3, TierPrefs.validate(max, "mm:item/input", Integer.MAX_VALUE));
        assertEquals(TierResolver.LOWEST, TierPrefs.validate(max, "mm:item/input", -7));
    }

    @Test
    void loadKeepsAtMostMaxEntries() {
        var tag = new CompoundTag();
        for (int i = 0; i < TierPrefs.MAX_ENTRIES + 10; i++) {
            tag.putInt("key" + i, 2);
        }
        assertEquals(TierPrefs.MAX_ENTRIES, TierPrefs.load(tag).asMap().size());
    }

    @Test
    void saveAndLoadRoundTrip() {
        var prefs = new TierPrefs();
        prefs.set("mm:item/input", 2);
        prefs.set("mm:fluid/output", 4);
        assertEquals(prefs.asMap(), TierPrefs.load(prefs.save()).asMap());
    }
}
