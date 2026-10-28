package io.ticticboom.mods.mm.controller.single;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.loader.ControllerLoader;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SingleMachines {

    private static final Set<Identifier> SUPPORTED = Set.of(Ref.Ports.ITEM, Ref.Ports.FLUID, Ref.Ports.ENERGY, Ref.Ports.MEK_CHEMICAL);
    private static final Map<String, List<SingleMachineSlot>> SLOTS = new HashMap<>();

    private SingleMachines() {
    }

    public static void define(String controllerId, List<SingleMachineSlot> slots) {
        if (slots.isEmpty()) {
            throw new RuntimeException(String.format("The single-block machine '%s' has no slots.", controllerId));
        }
        var ids = new HashSet<String>();
        for (SingleMachineSlot slot : slots) {
            if (!ids.add(slot.id())) {
                throw new RuntimeException(String.format("The single-block machine '%s' has two slots called '%s'.", controllerId, slot.id()));
            }
            if (!SUPPORTED.contains(slot.type())) {
                throw new RuntimeException(String.format("Slot '%s' of the single-block machine '%s' is %s, which single-block machines do not support. Supported types: %s",
                        slot.id(), controllerId, slot.type(), SUPPORTED.stream().map(Identifier::toString).sorted().collect(Collectors.joining(", "))));
            }
        }
        SLOTS.put(controllerId, List.copyOf(slots));
    }

    public static List<SingleMachineSlot> slots(ControllerModel model) {
        var slots = SLOTS.get(model.id());
        if (slots == null) {
            define(model.id(), parse(model));
            slots = SLOTS.get(model.id());
        }
        return slots;
    }

    private static List<SingleMachineSlot> parse(ControllerModel model) {
        var config = model.config();
        if (config == null || !config.has("slots") || !config.get("slots").isJsonArray()) {
            throw new RuntimeException(String.format("The single-block machine '%s' needs a \"slots\" list.", model.id()));
        }
        var result = new ArrayList<SingleMachineSlot>();
        for (JsonElement element : config.getAsJsonArray("slots")) {
            var json = element.getAsJsonObject();
            var id = json.get("id").getAsString();
            var type = Identifier.parse(json.get("type").getAsString());
            var input = json.get("input").getAsBoolean();
            var portConfig = json.has("config") ? json.getAsJsonObject("config") : new JsonObject();
            var factory = MMPortRegistry.requirePortType(type).getParser().parseStorage(portConfig);
            result.add(new SingleMachineSlot(id, type, input, factory));
        }
        return result;
    }

    public static boolean isSingle(ControllerModel model) {
        return Ref.Controller.SINGLE.equals(model.type());
    }

    public static boolean isImplicitStructure(Identifier structureId) {
        if (!Ref.ID.equals(structureId.getNamespace())) {
            return false;
        }
        var model = ControllerLoader.CONTROLLER_MODELS.get(structureId.getPath());
        return model != null && isSingle(model);
    }

    public static Map<Identifier, StructureModel> implicitStructures() {
        var result = new HashMap<Identifier, StructureModel>();
        for (ControllerModel model : ControllerLoader.CONTROLLER_MODELS.values()) {
            if (!isSingle(model)) {
                continue;
            }
            var id = Ref.id(model.id());
            var json = new JsonObject();
            var name = model.config() != null && model.config().has("name") ? model.config().get("name") : null;
            if (name != null) {
                json.add("name", name);
            } else {
                json.addProperty("name", model.id());
            }
            json.addProperty("controllerIds", id.toString());
            var layer = new JsonArray();
            layer.add("C");
            var layout = new JsonArray();
            layout.add(layer);
            json.add("layout", layout);
            json.add("key", new JsonObject());
            result.put(id, StructureModel.parse(json, id));
        }
        return result;
    }
}
