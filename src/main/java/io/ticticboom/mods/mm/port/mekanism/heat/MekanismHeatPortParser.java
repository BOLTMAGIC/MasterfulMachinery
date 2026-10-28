package io.ticticboom.mods.mm.port.mekanism.heat;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;

public class MekanismHeatPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new MekanismHeatPortStorageFactory(MekanismHeatPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new MekanismHeatPortIngredient(AmountRange.parse(json, "amount"));
    }
}
