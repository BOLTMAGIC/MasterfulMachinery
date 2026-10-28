package io.ticticboom.mods.mm.port.replication.matter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class ReplicationMatterPortStorageFactory implements IPortStorageFactory {

    private final ReplicationMatterPortStorageModel model;

    public ReplicationMatterPortStorageFactory(ReplicationMatterPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new ReplicationMatterPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("tanks", model.tanks());
        json.addProperty("priority", model.priority());
        var matter = new JsonArray();
        for (var entry : model.matter()) {
            matter.add(entry == null ? null : entry.toString());
        }
        json.add("matter", matter);
        json.addProperty("tierRank", model.getTierRank());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
