package io.ticticboom.mods.mm.port.replication.matter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public record ReplicationMatterPortStorageModel(
        int capacity,
        List<Identifier> matter,
        Supplier<Boolean> network,
        int priority,
        int tierRank
) implements IPortStorageModel {

    public static final int DEFAULT_CAPACITY = 1000;

    public static ReplicationMatterPortStorageModel parse(JsonObject json) {
        int capacity = json.has("capacity") ? json.get("capacity").getAsInt() : DEFAULT_CAPACITY;
        if (capacity <= 0) {
            capacity = DEFAULT_CAPACITY;
        }
        var matter = parseMatter(json);
        int tanks = json.has("tanks") ? json.get("tanks").getAsInt() : matter.size();
        while (matter.size() < Math.max(1, tanks)) {
            matter.add(null);
        }
        var network = ParserUtils.parseOrDefaultSupplier(json, "network", () -> true, JsonElement::getAsBoolean);
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        int tierRank = 0;
        if (json.has("tierRank")) {
            try {
                tierRank = json.get("tierRank").getAsInt();
            } catch (Exception ignored) {}
        }
        return new ReplicationMatterPortStorageModel(capacity, Collections.unmodifiableList(matter), network, clampPriority(priority), tierRank);
    }

    private static List<Identifier> parseMatter(JsonObject json) {
        var result = new ArrayList<Identifier>();
        if (!json.has("matter") || json.get("matter").isJsonNull()) {
            return result;
        }
        var element = json.get("matter");
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                result.add(ParserUtils.parseId(entry));
            }
            return result;
        }
        result.add(ParserUtils.parseId(element));
        return result;
    }

    public static int clampPriority(int priority) {
        return Math.max(0, Math.min(10, priority));
    }

    public int tanks() {
        return matter.size();
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
