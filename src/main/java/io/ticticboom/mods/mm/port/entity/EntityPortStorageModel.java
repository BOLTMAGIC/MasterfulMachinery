package io.ticticboom.mods.mm.port.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EntityPortStorageModel(
        int capacity,
        EntityPortMode mode,
        boolean invulnerable,
        boolean immobile,
        boolean silent,
        Boolean persistent,
        boolean consume,
        double speedPerEntity,
        int zoneWidth,
        int zoneHeight,
        int zoneDepth,
        List<Identifier> entities,
        List<Identifier> tags,
        int tierRank
) implements IPortStorageModel {

    public static final int DEFAULT_CAPACITY = 1;
    public static final int MAX_ZONE = 16;

    public static EntityPortStorageModel parse(JsonObject json) {
        int capacity = json.has("capacity") ? json.get("capacity").getAsInt() : DEFAULT_CAPACITY;
        if (capacity <= 0) {
            capacity = DEFAULT_CAPACITY;
        }
        var mode = EntityPortMode.parse(json.has("mode") && !json.get("mode").isJsonNull()
                ? json.get("mode").getAsString()
                : null);
        boolean invulnerable = json.has("invulnerable") && json.get("invulnerable").getAsBoolean();
        boolean immobile = !json.has("immobile") || json.get("immobile").getAsBoolean();
        boolean silent = json.has("silent") && json.get("silent").getAsBoolean();
        Boolean persistent = json.has("persistent") && !json.get("persistent").isJsonNull()
                ? json.get("persistent").getAsBoolean()
                : null;
        boolean consume = json.has("consume")
                ? json.get("consume").getAsBoolean()
                : mode == EntityPortMode.STORED;
        double speedPerEntity = json.has("speedPerEntity") ? json.get("speedPerEntity").getAsDouble() : 0;
        if (speedPerEntity < 0) {
            speedPerEntity = 0;
        }
        var zone = parseZone(json.get("zone"));
        return new EntityPortStorageModel(capacity, mode, invulnerable, immobile, silent, persistent, consume,
                speedPerEntity, zone[0], zone[1], zone[2],
                Collections.unmodifiableList(parseIds(json, "entities")),
                Collections.unmodifiableList(parseIds(json, "tags")),
                json.has("tierRank") ? json.get("tierRank").getAsInt() : 0);
    }

    private static int[] parseZone(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new int[] {1, 1, 1};
        }
        if (element.isJsonPrimitive()) {
            int size = element.getAsInt();
            return new int[] {clampZone(size), clampZone(size), clampZone(size)};
        }
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.size() != 3) {
                throw new RuntimeException(String.format(
                        "An MM entity port 'zone' array needs exactly three numbers (width, height, depth): %s", element));
            }
            return new int[] {
                    clampZone(array.get(0).getAsInt()),
                    clampZone(array.get(1).getAsInt()),
                    clampZone(array.get(2).getAsInt())};
        }
        var obj = element.getAsJsonObject();
        return new int[] {
                clampZone(obj.has("width") ? obj.get("width").getAsInt() : 1),
                clampZone(obj.has("height") ? obj.get("height").getAsInt() : 1),
                clampZone(obj.has("depth") ? obj.get("depth").getAsInt() : 1)};
    }

    public static int clampZone(int size) {
        return Math.max(1, Math.min(MAX_ZONE, size));
    }

    public boolean singleBlockZone() {
        return zoneWidth == 1 && zoneHeight == 1 && zoneDepth == 1;
    }

    private static List<Identifier> parseIds(JsonObject json, String field) {
        var result = new ArrayList<Identifier>();
        if (!json.has(field) || json.get(field).isJsonNull()) {
            return result;
        }
        var element = json.get(field);
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                result.add(ParserUtils.parseId(entry));
            }
            return result;
        }
        result.add(ParserUtils.parseId(element));
        return result;
    }

    public boolean accepts(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        if (entities.isEmpty() && tags.isEmpty()) {
            return true;
        }
        var id = EntityTypes.idOf(type);
        if (id != null && entities.contains(id)) {
            return true;
        }
        for (var tag : tags) {
            if (EntityTypes.inTag(type, tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
