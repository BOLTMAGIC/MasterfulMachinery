package io.ticticboom.mods.mm.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
