package io.ticticboom.mods.mm.port.energy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

import java.util.function.Supplier;

public record EnergyPortStorageModel(
        long capacity,
        long maxReceive,
        long maxExtract,
        Supplier<Boolean> autoPush,
        int tierRank
) implements IPortStorageModel {

    public static EnergyPortStorageModel parse(JsonObject json) {
        long capacity = json.get("capacity").getAsLong();
        long maxReceive = json.get("maxReceive").getAsLong();
        long maxExtract = json.get("maxExtract").getAsLong();
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        int tierRank = 0;
        if (json.has("tierRank")) {
            try {
                tierRank = json.get("tierRank").getAsInt();
            } catch (Exception ignored) {}
        }
        return new EnergyPortStorageModel(capacity, maxReceive, maxExtract, autoPush, tierRank);
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
