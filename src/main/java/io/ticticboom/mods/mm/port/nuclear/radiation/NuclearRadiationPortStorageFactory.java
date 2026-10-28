package io.ticticboom.mods.mm.port.nuclear.radiation;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class NuclearRadiationPortStorageFactory implements IPortStorageFactory {

    private final NuclearRadiationPortStorageModel model;

    public NuclearRadiationPortStorageFactory(NuclearRadiationPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new NuclearRadiationPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        return model.serialize();
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
