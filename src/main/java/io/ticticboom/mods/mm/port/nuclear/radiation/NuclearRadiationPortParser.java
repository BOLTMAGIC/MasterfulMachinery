package io.ticticboom.mods.mm.port.nuclear.radiation;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;

public class NuclearRadiationPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new NuclearRadiationPortStorageFactory(NuclearRadiationPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        String isotope = json.has("isotope") ? json.get("isotope").getAsString() : null;
        var element = json.get("amount");
        if (element == null || element.isJsonNull()) {
            throw new RuntimeException("Missing 'amount' on a radiation ingredient: " + json);
        }
        if (!element.isJsonObject()) {
            double amount = element.getAsDouble();
            return new NuclearRadiationPortIngredient(isotope, amount, amount, null);
        }
        var range = element.getAsJsonObject();
        if (!range.has("min") || !range.has("max")) {
            throw new RuntimeException("Ranged 'amount' on a radiation ingredient needs both 'min' and 'max': " + json);
        }
        double min = range.get("min").getAsDouble();
        double max = range.get("max").getAsDouble();
        if (max < min) {
            throw new RuntimeException("Ranged 'amount' on a radiation ingredient has max below min: " + json);
        }
        if (max == min) {
            return new NuclearRadiationPortIngredient(isotope, min, max, null);
        }
        String rollKey = range.has("rollGroup") && !range.get("rollGroup").isJsonNull()
                ? "g:" + range.get("rollGroup").getAsString()
                : AmountRange.nextRollKey();
        return new NuclearRadiationPortIngredient(isotope, min, max, rollKey);
    }
}
