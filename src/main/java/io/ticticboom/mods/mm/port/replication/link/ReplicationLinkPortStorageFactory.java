package io.ticticboom.mods.mm.port.replication.link;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class ReplicationLinkPortStorageFactory implements IPortStorageFactory {

    private final ReplicationLinkPortStorageModel model;

    public ReplicationLinkPortStorageFactory(ReplicationLinkPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new ReplicationLinkPortStorage(model);
    }

    @Override
    public JsonObject serialize() {
        return new JsonObject();
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
