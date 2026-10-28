package io.ticticboom.mods.mm.port.mekanism.heat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;
import mekanism.api.heat.HeatAPI;

import java.util.function.Supplier;

public record MekanismHeatPortStorageModel(
        int capacity,
        double heatCapacity,
        double inverseConduction,
        Supplier<Boolean> autoPush,
        int tierRank
) implements IPortStorageModel {

    public static final double DEFAULT_HEAT_CAPACITY = 1000.0;

    public static MekanismHeatPortStorageModel parse(JsonObject json) {
        int capacity = json.get("capacity").getAsInt();
        double heatCapacity = json.has("heatCapacity") ? json.get("heatCapacity").getAsDouble() : DEFAULT_HEAT_CAPACITY;
        if (heatCapacity <= 0) {
            heatCapacity = DEFAULT_HEAT_CAPACITY;
        }
        double inverseConduction = json.has("inverseConduction")
                ? json.get("inverseConduction").getAsDouble()
                : HeatAPI.DEFAULT_INVERSE_CONDUCTION;
        if (inverseConduction <= 0) {
            inverseConduction = HeatAPI.DEFAULT_INVERSE_CONDUCTION;
        }
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> true, JsonElement::getAsBoolean);
        int tierRank = 0;
        if (json.has("tierRank")) {
            try {
                tierRank = json.get("tierRank").getAsInt();
            } catch (Exception ignored) {}
        }
        return new MekanismHeatPortStorageModel(capacity, heatCapacity, inverseConduction, autoPush, tierRank);
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
