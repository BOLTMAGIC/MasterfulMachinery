package io.ticticboom.mods.mm.port.ars.source;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class ArsSourcePortStorageFactory implements IPortStorageFactory {

    private final ArsSourcePortStorageModel model;

    public ArsSourcePortStorageFactory(ArsSourcePortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new ArsSourcePortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("range", model.range());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
