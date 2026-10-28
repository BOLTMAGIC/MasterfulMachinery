package io.ticticboom.mods.mm.setup.loader;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.PortSides;
import io.ticticboom.mods.mm.port.PortType;

import java.util.List;
import io.ticticboom.mods.mm.util.ParserUtils;
import io.ticticboom.mods.mm.util.PortUtils;

public class PortLoader extends AbstractConfigLoader<PortModel> {

    public static void loadAll() {
        new PortLoader().load();
    }

    @Override
    protected String getConfigPath() {
        return "ports";
    }

    @Override
    protected List<PortModel> parseModels(JsonObject json) {
        // One file declares a port; MM registers it twice, once as the input side and once as the
        // output side.
        if (!PortUtils.sided(ParserUtils.parseId(json, "type"))) {
            return List.of(PortModel.parse(json, false));
        }
        var sides = PortSides.parse(json.get("only"));
        var models = new java.util.ArrayList<PortModel>(2);
        if (sides.hasInput()) {
            models.add(PortModel.parse(json, true));
        }
        if (sides.hasOutput()) {
            models.add(PortModel.parse(json, false));
        }
        return models;
    }

    @Override
    protected String getModelId(PortModel model) {
        return model.id();
    }

    @Override
    protected void registerModels(List<PortModel> portModels) {
        for (PortModel portModel : portModels) {
            PortType portType = MMPortRegistry.requirePortType(portModel.type());
            portType.register(portModel);
        }
        if (MMInteropManager.KUBEJS.isPresent()) {
            for (PortModel portModel : MMInteropManager.KUBEJS.get().postRegisterPorts()) {
                PortType portType = MMPortRegistry.requirePortType(portModel.type());
                portType.register(portModel);
            }
        }
        // rebuild the port controller index after all ports have been registered
        MMPortRegistry.rebuildPortCache();
    }
}
