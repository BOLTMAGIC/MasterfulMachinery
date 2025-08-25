package io.ticticboom.mods.mm.port.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.ParserUtils;

public class FluidPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var rows = json.get("rows").getAsInt();
        var columns = json.get("columns").getAsInt();
        var slotCapacity = json.get("slotCapacity").getAsInt();
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        return new FluidPortStorageFactory(new FluidPortStorageModel(rows, columns, slotCapacity, autoPush));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var fluidId = ParserUtils.parseId(json, "fluid");
        var amount = json.get("amount").getAsInt();
        return new FluidPortIngredient(fluidId, amount);
    }
}
