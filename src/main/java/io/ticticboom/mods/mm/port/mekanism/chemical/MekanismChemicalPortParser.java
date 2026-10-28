package io.ticticboom.mods.mm.port.mekanism.chemical;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import io.ticticboom.mods.mm.port.IPortIngredient;

public class MekanismChemicalPortParser implements IPortParser {

    public MekanismChemicalPortStorageFactory createFactory(long amount) {
        return new MekanismChemicalPortStorageFactory(new MekanismChemicalPortStorageModel(amount));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var chemical = parseChemicalId(json, "chemical");
        var amount = json.get("amount").getAsLong();
        return new MekanismChemicalPortIngredient(chemical, amount);
    }

    protected static Identifier parseChemicalId(JsonObject json, String preferred) {
        for (String key : new String[] {preferred, "chemical", "gas", "slurry", "pigment", "infuse"}) {
            var found = ParserUtils.parseOptionalId(json, key);
            if (found != null) {
                return found;
            }
        }
        throw new RuntimeException(String.format(
                "An MM Mekanism ingredient needs a 'chemical' field: %s", json));
    }

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new MekanismChemicalPortStorageFactory(MekanismChemicalPortStorageModel.parse(json));
    }
}
