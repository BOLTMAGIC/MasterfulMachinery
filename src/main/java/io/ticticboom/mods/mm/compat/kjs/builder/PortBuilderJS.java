package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonElement;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.compat.kjs.KubeJsValues;
import io.ticticboom.mods.mm.model.IdList;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.PortSides;
import io.ticticboom.mods.mm.util.ParserUtils;
import lombok.Getter;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import io.ticticboom.mods.mm.util.PortUtils;

@Getter
public class PortBuilderJS {

    private final String id;
    private Identifier type;
    private String name;
    private final List<Identifier> controllers = new ArrayList<>();
    private Consumer<PortConfigBuilderJS> builder;
    private PortSides sides = PortSides.BOTH;
    private String inputName;
    private String outputName;
    private JsonElement nameSpec;
    private JsonElement inputNameSpec;
    private JsonElement outputNameSpec;
    private final Map<String, String> textures = new LinkedHashMap<>();

    @HideFromJS
    public PortBuilderJS(String id) {
        this.id = id;
    }

    public PortBuilderJS overlay(String texture) {
        return putTexture("overlay", texture);
    }

    public PortBuilderJS inputOverlay(String texture) {
        return putTexture("inputOverlay", texture);
    }

    public PortBuilderJS outputOverlay(String texture) {
        return putTexture("outputOverlay", texture);
    }

    public PortBuilderJS texture(String texture) {
        return putTexture("texture", texture);
    }

    public PortBuilderJS inputTexture(String texture) {
        return putTexture("inputTexture", texture);
    }

    public PortBuilderJS outputTexture(String texture) {
        return putTexture("outputTexture", texture);
    }

    public PortBuilderJS model(String model) {
        return putTexture("model", model);
    }

    public PortBuilderJS inputModel(String model) {
        return putTexture("inputModel", model);
    }

    public PortBuilderJS outputModel(String model) {
        return putTexture("outputModel", model);
    }

    @HideFromJS
    private PortBuilderJS putTexture(String key, String texture) {
        var rl = Identifier.tryParse(texture);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + texture);
        textures.put(key, rl.toString());
        return this;
    }

    public PortBuilderJS inputName(Object name) {
        this.inputNameSpec = KubeJsValues.toJson(name);
        this.inputName = ParserUtils.parseComponentKey(inputNameSpec);
        return this;
    }

    public PortBuilderJS outputName(Object name) {
        this.outputNameSpec = KubeJsValues.toJson(name);
        this.outputName = ParserUtils.parseComponentKey(outputNameSpec);
        return this;
    }

    public PortBuilderJS only(String side) {
        this.sides = PortSides.parse(side);
        return this;
    }

    public PortBuilderJS name(Object name) {
        this.nameSpec = KubeJsValues.toJson(name);
        this.name = ParserUtils.parseComponentKey(nameSpec);
        return this;
    }

    @SuppressWarnings("unused")
    public PortBuilderJS controllerId(String controllerId) {
        var rl = Identifier.tryParse(controllerId);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + controllerId);
        controllers.add(rl);
        return this;
    }

    public PortBuilderJS config(String type, Consumer<PortConfigBuilderJS> builder) {
        var rl = Identifier.tryParse(type);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + type);
        this.type = rl;
        this.builder = builder;
        return this;
    }

    @HideFromJS
    public List<PortModel> build() {
        var portType = MMPortRegistry.requirePortType(type);
        int[] tierRank = {0};
        var storageFactory = portType.createStorageFactory(config -> {
            builder.accept(config);
            tierRank[0] = config.getTierRank();
        });
        IdList controllerIds = new IdList(controllers);
        var built = new ArrayList<PortModel>(2);
        if (!PortUtils.sided(type)) {
            var spec = outputNameSpec != null ? outputNameSpec : inputNameSpec;
            built.add(PortModel.createStyled(id, nameSpec, spec, controllerIds, type, storageFactory, false));
            built.forEach(port -> textures.forEach((key, value) -> port.jsonConfig().addProperty(key, value)));
            built.forEach(port -> recordTierRank(port, tierRank[0]));
            return built;
        }
        if (sides.hasInput()) {
            built.add(PortModel.createStyled(id, nameSpec, inputNameSpec, controllerIds, type, storageFactory, true));
        }
        if (sides.hasOutput()) {
            built.add(PortModel.createStyled(id, nameSpec, outputNameSpec, controllerIds, type, storageFactory, false));
        }
        built.forEach(port -> textures.forEach((key, value) -> port.jsonConfig().addProperty(key, value)));
        built.forEach(port -> recordTierRank(port, tierRank[0]));
        return built;
    }

    private static void recordTierRank(PortModel port, int tierRank) {
        if (tierRank <= 0) {
            return;
        }
        var json = port.jsonConfig();
        if (json.has("config") && json.get("config").isJsonObject()) {
            json.getAsJsonObject("config").addProperty("tierRank", tierRank);
        } else {
            json.addProperty("tierRank", tierRank);
        }
    }
}
