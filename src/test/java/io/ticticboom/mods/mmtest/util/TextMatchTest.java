package io.ticticboom.mods.mmtest.util;

import io.ticticboom.mods.mm.util.TextMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMatchTest {

    @Test
    void ignoresCase() {
        assertEquals(5, TextMatch.indexOfIgnoreCase("Auto Crusher T2", "CRUSH"));
        assertEquals(0, TextMatch.indexOfIgnoreCase("Auto Crusher", "auto"));
        assertEquals(-1, TextMatch.indexOfIgnoreCase("Auto Crusher", "press"));
    }

    @Test
    void emptyQueryMatchesAtStart() {
        assertEquals(0, TextMatch.indexOfIgnoreCase("anything", ""));
    }

    @Test
    void queryLongerThanText() {
        assertEquals(-1, TextMatch.indexOfIgnoreCase("ab", "abc"));
    }

    @Test
    void turkishDottedCapitalKeepsIndicesInsideTheName() {
        String name = "KİMYASAL İŞLEYİCİ";
        // lower-casing this name makes it longer; the index must still point into the original
        assertTrue(name.toLowerCase(java.util.Locale.ROOT).length() > name.length());
        int start = TextMatch.indexOfIgnoreCase(name, "ci");
        assertEquals(name.indexOf("Cİ"), start);
        assertTrue(start + 2 <= name.length());
        assertEquals("Cİ", name.substring(start, start + 2));
        assertEquals(1, TextMatch.indexOfIgnoreCase(name, "i"));
        assertEquals(9, TextMatch.indexOfIgnoreCase(name, "işl"));
    }

    @Test
    void turkishCapitalInQuery() {
        assertEquals(9, TextMatch.indexOfIgnoreCase("kimyasal işleyici", "İŞ"));
        assertTrue(TextMatch.containsIgnoreCase("mmtest:kimya", "KİMYA"));
        assertFalse(TextMatch.containsIgnoreCase("mmtest:kimya", "kimyasal"));
    }
}
