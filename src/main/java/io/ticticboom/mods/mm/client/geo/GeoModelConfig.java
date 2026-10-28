package io.ticticboom.mods.mm.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public record GeoModelConfig(Identifier model, Identifier texture, @Nullable Identifier animations,
                             @Nullable String idle, @Nullable String working,
                             double offsetX, double offsetY, double offsetZ, float scale, boolean rotate, boolean glow) {

    public static final String KEY = "geoModel";
    public static final String NONE = "none";

    private static final Map<JsonElement, Optional<GeoModelConfig>> CACHE = new IdentityHashMap<>();

    public static GeoModelConfig parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("geoModel must be an object (or \"none\" on a structure)");
        }
        JsonObject o = json.getAsJsonObject();
        Identifier model = id(o, "model", true);
        Identifier texture = id(o, "texture", true);
        Identifier animations = id(o, "animations", false);
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
    private static Identifier id(JsonObject o, String key, boolean required) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            if (required) {
                throw new IllegalArgumentException("geoModel needs '" + key + "'");
            }
            return null;
        }
        Identifier id = Identifier.tryParse(o.get(key).getAsString());
        if (id == null) {
            throw new IllegalArgumentException("geoModel '" + key + "' is not a valid id: " + o.get(key).getAsString());
        }
        return id;
    }

    @Nullable
    private static String text(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        String s = o.get(key).getAsString();
        return s.isBlank() ? null : s;
    }

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
                    Ref.LOG.warn("Ignoring geoModel {}: {}", e, ex.getMessage());
                    return Optional.empty();
                }
            }).orElse(null);
        }
    }

    public double[] worldOffset(Direction facing) {
        if (!rotate) {
            return new double[]{offsetX, offsetY, offsetZ};
        }
        Direction right = facing.getClockWise();
        return new double[]{
                right.getStepX() * offsetX + facing.getStepX() * offsetZ,
                offsetY,
                right.getStepZ() * offsetX + facing.getStepZ() * offsetZ
        };
    }

    public float modelYRot(Direction facing) {
        return rotate ? 0f - facing.toYRot() : 0f;
    }
}
