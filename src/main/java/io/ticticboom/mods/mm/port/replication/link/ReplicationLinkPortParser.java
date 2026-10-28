package io.ticticboom.mods.mm.port.replication.link;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;

public class ReplicationLinkPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new ReplicationLinkPortStorageFactory(new ReplicationLinkPortStorageModel());
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        throw new RuntimeException("Replication link ports can't be used as recipe ingredients");
    }
}
