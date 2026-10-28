package io.ticticboom.mods.mmtest.recipe;

import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.flex.RecipeMode;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlexTemplateTest {

    @Test
    void seedsToEssence() {
        var template = FlexTemplate.parse("(.+)_seeds", "{ns}:$1_essence");
        assertEquals("mysticalagriculture:iron_essence", template.apply("mysticalagriculture", "iron_seeds"));
    }

    @Test
    void notMatchingPathGivesNull() {
        var template = FlexTemplate.parse("(.+)_seeds", "{ns}:$1_essence");
        assertNull(template.apply("mysticalagriculture", "iron_essence"));
    }

    @Test
    void matchMustCoverTheWholePath() {
        var template = FlexTemplate.parse("seeds", "{ns}:x");
        assertNull(template.apply("a", "iron_seeds"));
    }

    @Test
    void defaultMatchTakesWholePath() {
        var template = FlexTemplate.parse(null, "other:{path}");
        assertEquals("other:iron_seeds", template.apply("a", "iron_seeds"));
        assertEquals("other:iron_seeds", FlexTemplate.parse(null, "other:$1").apply("a", "iron_seeds"));
    }

    @Test
    void manyGroupsDoNotClash() {
        var template = FlexTemplate.parse("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)", "x:$10_$1");
        assertEquals("x:j_a", template.apply("ns", "abcdefghij"));
    }

    @Test
    void missingItemTemplateIsAnError() {
        assertThrows(RuntimeException.class, () -> FlexTemplate.parse("(.*)", ""));
    }

    @Test
    void badRegexIsAnError() {
        assertThrows(RuntimeException.class, () -> FlexTemplate.parse("(", "x:y"));
    }

    @Test
    void modeDefaultsToFixed() {
        assertEquals(RecipeMode.FIXED, RecipeMode.parse(new JsonObject()));
        var json = new JsonObject();
        json.addProperty("mode", "Flex");
        assertEquals(RecipeMode.FLEX, RecipeMode.parse(json));
        json.addProperty("mode", "dynamic");
        assertThrows(RuntimeException.class, () -> RecipeMode.parse(json));
    }
}
