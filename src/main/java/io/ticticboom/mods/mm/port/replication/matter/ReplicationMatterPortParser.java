package io.ticticboom.mods.mm.port.replication.matter;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class ReplicationMatterPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new ReplicationMatterPortStorageFactory(ReplicationMatterPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new ReplicationMatterPortIngredient(ParserUtils.parseId(json, "matter"), AmountRange.parse(json, "amount"));
    }
}
