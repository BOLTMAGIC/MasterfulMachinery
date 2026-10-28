package io.ticticboom.mods.mm.port.mekanism.chemical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

import java.util.function.Supplier;

public record MekanismChemicalPortStorageModel(
        long amount,
        Supplier<Boolean> autoPush
) implements IPortStorageModel {

    public MekanismChemicalPortStorageModel(long amount) {
        this(amount, () -> MMConfig.DEFAULT_PORT_AUTO_PUSH);
    }

    public static MekanismChemicalPortStorageModel parse(JsonObject json) {
        var amount = json.get("capacity").getAsLong();
        var autoPush = ParserUtils.parseOrDefaultSupplier(json, "autoPush",
                () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        return new MekanismChemicalPortStorageModel(amount, autoPush);
    }
}
