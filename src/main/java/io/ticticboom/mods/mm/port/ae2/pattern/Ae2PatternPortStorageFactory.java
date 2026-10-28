package io.ticticboom.mods.mm.port.ae2.pattern;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class Ae2PatternPortStorageFactory implements IPortStorageFactory {

    private final Ae2PatternPortStorageModel model;

    public Ae2PatternPortStorageFactory(Ae2PatternPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new Ae2PatternPortStorage(model);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("patternPriority", model.patternPriority());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
