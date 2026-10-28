package io.ticticboom.mods.mm.port.projecte.emc;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.LongAmountRange;

public class ProjectEEmcPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var capacity = json.get("capacity").getAsLong();
        var kleinSlot = json.has("kleinSlot") && json.get("kleinSlot").getAsBoolean();
        var kleinRate = json.has("kleinRate") ? json.get("kleinRate").getAsLong() : 0;
        return new ProjectEEmcPortStorageFactory(new ProjectEEmcPortStorageModel(capacity, kleinSlot, kleinRate));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new ProjectEEmcPortIngredient(LongAmountRange.parse(json, "emc"));
    }
}
