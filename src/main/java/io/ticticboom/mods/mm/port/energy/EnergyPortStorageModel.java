package io.ticticboom.mods.mm.port.energy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

import java.util.function.Supplier;

public record EnergyPortStorageModel(
        int capacity,
        int maxReceive,
        int maxExtract,
        Supplier<Boolean> autoPush
) implements IPortStorageModel {

    public static EnergyPortStorageModel parse(JsonObject json) {
        int capacity = json.get("capacity").getAsInt();
        int maxReceive = json.get("maxReceive").getAsInt();
        int maxExtract = json.get("maxExtract").getAsInt();
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        return new EnergyPortStorageModel(capacity, maxReceive, maxExtract, autoPush);
    }
}
