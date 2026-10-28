package io.ticticboom.mods.mm.port.fluid;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class FluidPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var rows = json.get("rows").getAsInt();
        var columns = json.get("columns").getAsInt();
        var slotCapacity = json.get("slotCapacity").getAsInt();
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        int tierRank = 0;
        if (json.has("tierRank")) {
            try {
                tierRank = json.get("tierRank").getAsInt();
            } catch (Exception ignored) {}
        }
        var fluids = new ArrayList<String>();
        if (json.has("fluids")) {
            var element = json.get("fluids");
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(entry -> fluids.add(entry.getAsString()));
            } else {
                fluids.add(element.getAsString());
            }
        }
        return new FluidPortStorageFactory(new FluidPortStorageModel(rows, columns, slotCapacity, autoPush, tierRank, List.copyOf(fluids)));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var fluidId = ParserUtils.parseId(json, "fluid");
        var amount = AmountRange.parse(json, "amount");
        return new FluidPortIngredient(fluidId, amount);
    }
}
