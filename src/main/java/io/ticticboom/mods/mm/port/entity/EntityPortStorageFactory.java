package io.ticticboom.mods.mm.port.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class EntityPortStorageFactory implements IPortStorageFactory {

    private final EntityPortStorageModel model;

    public EntityPortStorageFactory(EntityPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new EntityPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("mode", model.mode().serialize());
        json.addProperty("invulnerable", model.invulnerable());
        json.addProperty("immobile", model.immobile());
        json.addProperty("silent", model.silent());
        json.addProperty("persistent", model.persistent());
        json.addProperty("consume", model.consume());
        json.addProperty("speedPerEntity", model.speedPerEntity());
        var zone = new JsonObject();
        zone.addProperty("width", model.zoneWidth());
        zone.addProperty("height", model.zoneHeight());
        zone.addProperty("depth", model.zoneDepth());
        json.add("zone", zone);
        json.add("entities", ids(model.entities()));
        json.add("tags", ids(model.tags()));
        json.addProperty("tierRank", model.getTierRank());
        return json;
    }

    private static JsonArray ids(java.util.List<net.minecraft.resources.Identifier> values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(value.toString());
        }
        return array;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}
