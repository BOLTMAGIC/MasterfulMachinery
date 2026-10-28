package io.ticticboom.mods.mm.port.entity;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class EntityPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new EntityPortStorageFactory(EntityPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var entity = ParserUtils.parseOptionalId(json, "entity");
        var tag = ParserUtils.parseOptionalId(json, "tag");
        if (entity == null && tag == null) {
            throw new RuntimeException(String.format(
                    "An MM entity ingredient needs either 'entity' or 'tag': %s", json));
        }
        var amount = json.has("amount") ? AmountRange.parse(json, "amount") : AmountRange.of(1);
        return new EntityPortIngredient(entity, tag, amount);
    }
}
