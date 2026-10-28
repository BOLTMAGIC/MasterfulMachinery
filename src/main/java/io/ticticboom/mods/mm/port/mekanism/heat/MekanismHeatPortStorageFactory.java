package io.ticticboom.mods.mm.port.mekanism.heat;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class MekanismHeatPortStorageFactory implements IPortStorageFactory {

    private final MekanismHeatPortStorageModel model;

    public MekanismHeatPortStorageFactory(MekanismHeatPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new MekanismHeatPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("heatCapacity", model.heatCapacity());
        json.addProperty("inverseConduction", model.inverseConduction());
        json.addProperty("tierRank", model.getTierRank());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
