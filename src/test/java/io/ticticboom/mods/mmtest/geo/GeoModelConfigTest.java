package io.ticticboom.mods.mmtest.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoModelConfigTest {

    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static final String MIN = "{\"model\":\"mm:geo/x.geo.json\",\"texture\":\"mm:textures/x.png\"}";

    @Test
    void minimalHasDefaults() {
        var c = GeoModelConfig.parse(obj(MIN));
        assertEquals("mm:geo/x.geo.json", c.model().toString());
        assertEquals("mm:textures/x.png", c.texture().toString());
        assertNull(c.animations());
        assertNull(c.idle());
        assertNull(c.working());
        assertEquals(0, c.offsetX());
        assertEquals(0, c.offsetY());
        assertEquals(0, c.offsetZ());
        assertEquals(1f, c.scale());
        assertTrue(c.rotate());
        assertFalse(c.glow());
    }

    @Test
    void fullConfig() {
        var c = GeoModelConfig.parse(obj("{\"model\":\"a:geo/b.geo.json\",\"texture\":\"a:t.png\",\"animations\":\"a:animations/b.animation.json\","
                + "\"idle\":\"animation.b.idle\",\"working\":\"animation.b.working\",\"offset\":[1,2.5,-3],\"scale\":2,\"rotate\":false,\"glow\":true}"));
        assertEquals("a:animations/b.animation.json", c.animations().toString());
        assertEquals("animation.b.idle", c.idle());
        assertEquals("animation.b.working", c.working());
        assertEquals(1, c.offsetX());
        assertEquals(2.5, c.offsetY());
        assertEquals(-3, c.offsetZ());
        assertEquals(2f, c.scale());
        assertFalse(c.rotate());
        assertTrue(c.glow());
    }

    @Test
    void badConfigsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(obj("{\"texture\":\"mm:t.png\"}")));
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(obj("{\"model\":\"mm:m\"}")));
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(obj("{\"model\":\"Bad Id\",\"texture\":\"mm:t.png\"}")));
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(obj("{\"model\":\"mm:m\",\"texture\":\"mm:t\",\"scale\":0}")));
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(obj("{\"model\":\"mm:m\",\"texture\":\"mm:t\",\"offset\":[1,2]}")));
        assertThrows(IllegalArgumentException.class, () -> GeoModelConfig.parse(JsonParser.parseString("\"mm:m\"")));
    }

    @Test
    void structureOverridesController() {
        var controller = obj("{\"geoModel\":" + MIN + "}");
        var structure = obj("{\"geoModel\":{\"model\":\"s:geo/s.geo.json\",\"texture\":\"s:t.png\"}}");
        assertEquals("s:geo/s.geo.json", GeoModelConfig.select(controller, structure).getAsJsonObject().get("model").getAsString());
        assertEquals("mm:geo/x.geo.json", GeoModelConfig.select(controller, obj("{}")).getAsJsonObject().get("model").getAsString());
        assertEquals("mm:geo/x.geo.json", GeoModelConfig.select(controller, null).getAsJsonObject().get("model").getAsString());
        assertNull(GeoModelConfig.select(controller, obj("{\"geoModel\":\"none\"}")));
        assertNull(GeoModelConfig.select(obj("{}"), null));
        assertNull(GeoModelConfig.select(null, null));
    }

    @Test
    void resolveReturnsNullForBadConfig() {
        assertNull(GeoModelConfig.resolve(obj("{\"geoModel\":{\"model\":\"mm:m\"}}"), null));
        assertNotNull(GeoModelConfig.resolve(obj("{\"geoModel\":" + MIN + "}"), null));
    }

    @Test
    void offsetFollowsTheController() {
        // x = right of a player in front of the controller, y = up, z = into the machine (= FACING)
        var c = GeoModelConfig.parse(obj("{\"model\":\"mm:m\",\"texture\":\"mm:t\",\"offset\":[1,2,3]}"));
        assertArrayEquals(new double[]{1, 2, -3}, c.worldOffset(Direction.NORTH));
        assertArrayEquals(new double[]{-1, 2, 3}, c.worldOffset(Direction.SOUTH));
        assertArrayEquals(new double[]{3, 2, 1}, c.worldOffset(Direction.EAST));
        assertArrayEquals(new double[]{-3, 2, -1}, c.worldOffset(Direction.WEST));
    }

    @Test
    void offsetIsWorldAxesWithoutRotate() {
        var c = GeoModelConfig.parse(obj("{\"model\":\"mm:m\",\"texture\":\"mm:t\",\"offset\":[1,2,3],\"rotate\":false}"));
        assertArrayEquals(new double[]{1, 2, 3}, c.worldOffset(Direction.WEST));
        assertEquals(0f, c.modelYRot(Direction.WEST));
    }

    @Test
    void modelFrontFacesThePlayer() {
        // Blockbench north (-z) must point out of the controller's front, so model +z points to FACING
        var c = GeoModelConfig.parse(obj(MIN));
        assertEquals(0f, c.modelYRot(Direction.SOUTH));
        assertEquals(-180f, c.modelYRot(Direction.NORTH));
        assertEquals(-270f, c.modelYRot(Direction.EAST));
        assertEquals(-90f, c.modelYRot(Direction.WEST));
    }
}
