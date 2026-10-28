package io.ticticboom.mods.mm.port.projecte.emc;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class ProjectEEmcPortStorageFactory implements IPortStorageFactory {

    private final ProjectEEmcPortStorageModel model;

    public ProjectEEmcPortStorageFactory(ProjectEEmcPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new ProjectEEmcPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("kleinSlot", model.kleinSlot());
        json.addProperty("kleinRate", model.kleinRate());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
