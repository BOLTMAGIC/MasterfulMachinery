package io.ticticboom.mods.mm.port.ars.source;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;

public class ArsSourcePortParser implements IPortParser {

    private static final int DEFAULT_RANGE = 6;

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var capacity = json.get("capacity").getAsInt();
        var range = json.has("range") ? json.get("range").getAsInt() : DEFAULT_RANGE;
        return new ArsSourcePortStorageFactory(new ArsSourcePortStorageModel(capacity, range));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new ArsSourcePortIngredient(AmountRange.parse(json, "source"));
    }
}
