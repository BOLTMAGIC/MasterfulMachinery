package io.ticticboom.mods.mmtest.recipe;

import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlexCostTest {

    @Test
    void linearMultipliesByKinds() {
        assertEquals(1, FlexCost.LINEAR.multiplier(1));
        assertEquals(10, FlexCost.LINEAR.multiplier(10));
        assertEquals(1, FlexCost.LINEAR.multiplier(0));
    }

    @Test
    void onceIsAlwaysOne() {
        var cost = FlexCost.of(FlexCost.Mode.ONCE, List.of());
        assertEquals(1, cost.multiplier(1));
        assertEquals(1, cost.multiplier(10));
    }

    @Test
    void tableUsesTheStepAtOrBelow() {
        var cost = FlexCost.of(FlexCost.Mode.TABLE, List.of("1=1", "4=2", "7=3.5"));
        assertEquals(1, cost.multiplier(3));
        assertEquals(2, cost.multiplier(4));
        assertEquals(2, cost.multiplier(6));
        assertEquals(3.5, cost.multiplier(20));
    }

    @Test
    void tableBelowTheFirstStepUsesTheFirstStep() {
        var cost = FlexCost.of(FlexCost.Mode.TABLE, List.of("3=2"));
        assertEquals(2, cost.multiplier(1));
    }

    @Test
    void stepFormat() {
        assertTrue(FlexCost.isStep("4=2"));
        assertTrue(FlexCost.isStep(" 4 = 2.5 "));
        assertFalse(FlexCost.isStep("4"));
        assertFalse(FlexCost.isStep("0=1"));
        assertFalse(FlexCost.isStep("a=b"));
    }

    @Test
    void recipeOverride() {
        assertNull(FlexCost.parse(JsonParser.parseString("{}").getAsJsonObject()));
        assertEquals(FlexCost.Mode.ONCE, FlexCost.parse(JsonParser.parseString("{\"flex_cost\": \"once\"}").getAsJsonObject()).mode());
        var table = FlexCost.parse(JsonParser.parseString("{\"flex_cost\": {\"mode\": \"table\", \"table\": {\"1\": 1, \"4\": 2}}}").getAsJsonObject());
        assertEquals(2, table.multiplier(5));
        var list = FlexCost.parse(JsonParser.parseString("{\"flex_cost\": {\"table\": [\"1=1\", \"2=1.5\"]}}").getAsJsonObject());
        assertEquals(1.5, list.multiplier(2));
        assertThrows(RuntimeException.class, () -> FlexCost.parse(JsonParser.parseString("{\"flex_cost\": \"cheap\"}").getAsJsonObject()));
        assertThrows(RuntimeException.class, () -> FlexCost.parse(JsonParser.parseString("{\"flex_cost\": {\"mode\": \"table\"}}").getAsJsonObject()));
    }
}
