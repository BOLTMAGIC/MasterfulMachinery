package io.ticticboom.mods.mm.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.util.ParserUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public record PortModel(
        String id,
        String name,
        Supplier<Component> displayNameSource,
        IdList controllerIds,
        Identifier type,
        IPortStorageFactory config,
        JsonObject jsonConfig,
        boolean input) {

    public Component displayName() {
        return displayNameSource.get();
    }

    public Identifier overlayTexture() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputOverlay" : "outputOverlay");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "overlay");
    }

    public Identifier baseTexture() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputTexture" : "outputTexture");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "texture");
    }

    public Identifier customModel() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputModel" : "outputModel");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "model");
    }

    public static PortModel parse(JsonObject json, boolean input) {
        var portTypeId = ParserUtils.parseId(json, "type");
        var sided = PortUtils.sided(portTypeId);
        var id = PortUtils.id(json.get("id").getAsString(), input, sided);
        // "name" accepts a plain string, a { "translation": "key" } object or a full text
        // component. "inputName" and "outputName" replace the whole name for that side, suffix
        // included, for packs that do not want "<name> Input".
        var sideName = json.get(input ? "inputName" : "outputName");
        String name;
        Supplier<Component> displayName;
        if (sideName != null && !sideName.isJsonNull()) {
            name = ParserUtils.parseComponentKey(sideName);
            displayName = ParserUtils.parseNameSupplier(sideName);
        } else {
            var base = ParserUtils.parseNameSupplier(json.get("name"));
            if (sided) {
                name = PortUtils.name(ParserUtils.parseComponentKey(json.get("name")), input);
                displayName = () -> PortUtils.name(base.get(), input);
            } else {
                name = ParserUtils.parseComponentKey(json.get("name"));
                displayName = base;
            }
        }
        var controllerIds = IdList.parse(json.get("controllerIds"));
        var type = portTypeId;
        var portType = MMPortRegistry.requirePortType(type);
        var storageFactory = portType.getParser().parseStorage(json.get("config").getAsJsonObject());
        return new PortModel(id, name, displayName, controllerIds, type, storageFactory, json, input);
    }

    public static PortModel create(String id, String name, IdList controllerIds, Identifier type, IPortStorageFactory config, boolean input) {
        return create(id, name, null, controllerIds, type, config, input);
    }

    public static PortModel create(String id, String name, String sideName, IdList controllerIds, Identifier type, IPortStorageFactory config, boolean input) {
        var sided = PortUtils.sided(type);
        var fid = PortUtils.id(id, input, sided);
        var fname = sideName != null ? sideName : (sided ? PortUtils.name(name, input) : name);
        var json = paramsToJson(fid, fname, controllerIds, type, config, input);
        var literal = Component.literal(fname);
        return new PortModel(fid, fname, () -> literal, controllerIds, type, config, json, input);
    }

    public static PortModel createStyled(String id, JsonElement nameSpec, JsonElement sideSpec, IdList controllerIds, Identifier type, IPortStorageFactory config, boolean input) {
        var sided = PortUtils.sided(type);
        var fid = PortUtils.id(id, input, sided);
        String fname;
        Supplier<Component> display;
        if (sideSpec != null && !sideSpec.isJsonNull()) {
            fname = ParserUtils.parseComponentKey(sideSpec);
            display = ParserUtils.parseNameSupplier(sideSpec);
        } else if (nameSpec != null && !nameSpec.isJsonNull()) {
            var base = ParserUtils.parseNameSupplier(nameSpec);
            if (sided) {
                fname = PortUtils.name(ParserUtils.parseComponentKey(nameSpec), input);
                display = () -> PortUtils.name(base.get(), input);
            } else {
                fname = ParserUtils.parseComponentKey(nameSpec);
                display = base;
            }
        } else {
            fname = PortUtils.name((String) null, input);
            var literal = Component.literal(fname);
            display = () -> literal;
        }
        var json = paramsToJson(fid, fname, controllerIds, type, config, input);
        return new PortModel(fid, fname, display, controllerIds, type, config, json, input);
    }

    public static JsonObject paramsToJson(String id, String name, IdList controllerIds, Identifier type, IPortStorageFactory config, boolean input) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.add("controllerIds", controllerIds.serialize());
        json.addProperty("type", type.toString());
        try {
            json.add("config", config.serialize());
        } catch (Exception e) {
            json.addProperty("config", config.toString());
        }
        return json;
    }

}
