package io.ticticboom.mods.mm.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A machine's GeckoLib model, from "geoModel" in its controller's or structure's JSON (KubeJS: .geoModel({...})).
 * The structure's "geoModel" replaces the controller's; "none" turns it off. Read when drawn, so a bad entry only
 * hides the model (with one warning) instead of stopping the world from loading. No GeckoLib types here.
 *
 * @param offsetX blocks to the right of a player standing in front of the controller (world x without rotate)
 * @param offsetY blocks up
 * @param offsetZ blocks into the machine, away from the controller's front (world z without rotate)
 * @param rotate  turn the model and the offset with the controller's facing
 */
public record GeoModelConfig(ResourceLocation model, ResourceLocation texture, @Nullable ResourceLocation animations,
                             @Nullable String idle, @Nullable String working,
                             double offsetX, double offsetY, double offsetZ, float scale, boolean rotate, boolean glow) {

    public static final String KEY = "geoModel";
    public static final String NONE = "none";
    // its own logger: Ref isn't needed (and may not load) in unit tests
    private static final Logger LOG = LogUtils.getLogger();

    // parsed per JSON element (identity: the element lives as long as its controller/structure definition)
    private static final Map<JsonElement, Optional<GeoModelConfig>> CACHE = new IdentityHashMap<>();

    public static GeoModelConfig parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("geoModel must be an object (or \"none\" on a structure)");
        }
        JsonObject o = json.getAsJsonObject();
        ResourceLocation model = id(o, "model", true);
        ResourceLocation texture = id(o, "texture", true);
        ResourceLocation animations = id(o, "animations", false);
        String idle = text(o, "idle");
        String working = text(o, "working");
        double[] offset = {0, 0, 0};
        if (o.has("offset")) {
            JsonElement el = o.get("offset");
            if (!el.isJsonArray() || el.getAsJsonArray().size() != 3) {
                throw new IllegalArgumentException("geoModel 'offset' must be [x, y, z]");
            }
            JsonArray a = el.getAsJsonArray();
            for (int i = 0; i < 3; i++) {
                offset[i] = a.get(i).getAsDouble();
            }
        }
        float scale = o.has("scale") ? o.get("scale").getAsFloat() : 1f;
        if (!(scale > 0)) {
            throw new IllegalArgumentException("geoModel 'scale' must be above 0");
        }
        boolean rotate = !o.has("rotate") || o.get("rotate").getAsBoolean();
        boolean glow = o.has("glow") && o.get("glow").getAsBoolean();
        return new GeoModelConfig(model, texture, animations, idle, working, offset[0], offset[1], offset[2], scale, rotate, glow);
    }

    @Nullable
    private static ResourceLocation id(JsonObject o, String key, boolean required) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            if (required) throw new IllegalArgumentException("geoModel needs '" + key + "'");
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(o.get(key).getAsString());
        if (id == null) {
            throw new IllegalArgumentException("geoModel '" + key + "' is not a valid id: " + o.get(key).getAsString());
        }
        return id;
    }

    @Nullable
    private static String text(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return null;
        String s = o.get(key).getAsString();
        return s.isBlank() ? null : s;
    }

    /** The "geoModel" that applies: the structure's if it has one, else the controller's; null if none / "none". */
    @Nullable
    public static JsonElement select(@Nullable JsonObject controllerConfig, @Nullable JsonObject structureConfig) {
        JsonElement el = null;
        if (structureConfig != null && structureConfig.has(KEY)) {
            el = structureConfig.get(KEY);
        } else if (controllerConfig != null && controllerConfig.has(KEY)) {
            el = controllerConfig.get(KEY);
        }
        if (el == null || el.isJsonNull() || (el.isJsonPrimitive() && NONE.equalsIgnoreCase(el.getAsString()))) {
            return null;
        }
        return el;
    }

    /** {@link #select} + {@link #parse}, remembered per JSON element; a bad entry warns once and gives null. */
    @Nullable
    public static GeoModelConfig resolve(@Nullable JsonObject controllerConfig, @Nullable JsonObject structureConfig) {
        JsonElement el = select(controllerConfig, structureConfig);
        if (el == null) {
            return null;
        }
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(el, e -> {
                try {
                    return Optional.of(parse(e));
                } catch (RuntimeException ex) {
                    LOG.warn("Ignoring geoModel {}: {}", e, ex.getMessage());
                    return Optional.empty();
                }
            }).orElse(null);
        }
    }

    /** Where the model's origin goes, relative to the bottom-center of the controller block. */
    public double[] worldOffset(Direction facing) {
        if (!rotate) {
            return new double[]{offsetX, offsetY, offsetZ};
        }
        // a player in front of the controller looks along FACING; their right is FACING turned clockwise
        Direction right = facing.getClockWise();
        return new double[]{
                right.getStepX() * offsetX + facing.getStepX() * offsetZ,
                offsetY,
                right.getStepZ() * offsetX + facing.getStepZ() * offsetZ
        };
    }

    /** Degrees around Y so the model's +z points to FACING (its Blockbench north faces out of the controller). */
    public float modelYRot(Direction facing) {
        // 0f - x, not -x: facing south must give 0, not -0
        return rotate ? 0f - facing.toYRot() : 0f;
    }
}
