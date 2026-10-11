package io.ticticboom.mods.mmtest.recipe;

import io.ticticboom.mods.mm.recipe.flex.FlexItems;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlexItemsTest {

    @Test
    void knownTokensAndIdTemplatesAreValid() {
        assertDoesNotThrow(() -> FlexItems.validate("{ma:crux}"));
        assertDoesNotThrow(() -> FlexItems.validate("{ma:essence}"));
        assertDoesNotThrow(() -> FlexItems.validate("{ns}:$1_essence"));
        assertDoesNotThrow(() -> FlexItems.validate("minecraft:{path}"));
        assertDoesNotThrow(() -> FlexItems.validate("{path}"));
    }

    @Test
    void unknownTokenIsAnError() {
        assertThrows(RuntimeException.class, () -> FlexItems.validate("{ma:seeds}"));
        assertThrows(RuntimeException.class, () -> FlexItems.validate("{crux}"));
    }
}
