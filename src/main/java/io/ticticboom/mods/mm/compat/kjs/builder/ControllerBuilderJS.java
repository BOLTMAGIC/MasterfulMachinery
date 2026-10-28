package io.ticticboom.mods.mm.compat.kjs.builder;

import io.ticticboom.mods.mm.util.ColorUtil;
import com.google.gson.JsonElement;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.compat.kjs.KubeJsValues;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.util.ParserUtils;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.single.SingleMachineSlot;
import io.ticticboom.mods.mm.controller.single.SingleMachines;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;

@Getter
public class ControllerBuilderJS {
    private final String id;
    private String name;
    private JsonElement nameSpec;
    private Identifier type;
    private boolean parallelProcessingDefault = false;
    private int maxParallelRecipes = -1;
    private RecipeSelectionMode recipeSelectionMode = RecipeSelectionMode.DEFAULT;
    private final Map<String, String> textures = new LinkedHashMap<>();
    @Getter(AccessLevel.NONE)
    private final List<PendingSlot> slots = new ArrayList<>();
    private JsonElement geoModel;

    private record PendingSlot(String id, Identifier type, boolean input, Consumer<PortConfigBuilderJS> config) {
    }

    @HideFromJS
    public ControllerBuilderJS(String id) {
        this.id = id;
    }

    public ControllerBuilderJS type(String id) {
        var rl = Identifier.tryParse(id);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + id);
        this.type = rl;
        return this;
    }

    public ControllerBuilderJS slot(String id, String type, boolean input, Consumer<PortConfigBuilderJS> config) {
        var rl = Identifier.tryParse(type);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + type);
        slots.add(new PendingSlot(id, rl, input, config));
        return this;
    }

    public ControllerBuilderJS slot(String id, String type, boolean input) {
        return slot(id, type, input, config -> {
        });
    }

    public ControllerBuilderJS overlay(String texture) {
        return putTexture("overlay", texture);
    }

    public ControllerBuilderJS texture(String texture) {
        return putTexture("texture", texture);
    }

    public ControllerBuilderJS model(String model) {
        return putTexture("model", model);
    }

    public ControllerBuilderJS idleOverlay(String texture) {
        return putTexture("idleOverlay", texture);
    }

    public ControllerBuilderJS workingOverlay(String texture) {
        return putTexture("workingOverlay", texture);
    }

    public ControllerBuilderJS idleModel(String model) {
        return putTexture("idleModel", model);
    }

    public ControllerBuilderJS workingModel(String model) {
        return putTexture("workingModel", model);
    }

    @HideFromJS
    private ControllerBuilderJS putTexture(String key, String texture) {
        var rl = Identifier.tryParse(texture);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + texture);
        textures.put(key, rl.toString());
        return this;
    }

    public ControllerBuilderJS workingSound(String sound) {
        return putTexture("workingSound", sound);
    }

    public ControllerBuilderJS workingSoundInterval(int ticks) {
        if (ticks < 1) throw new IllegalArgumentException("workingSoundInterval must be at least 1 tick: " + ticks);
        textures.put("workingSoundInterval", Integer.toString(ticks));
        return this;
    }

    public ControllerBuilderJS geoModel(JsonElement geoModel) {
        this.geoModel = geoModel;
        return this;
    }

    public ControllerBuilderJS workingParticle(String particle) {
        return putTexture("workingParticle", particle);
    }

    public ControllerBuilderJS unformedColor(String color) {
        return putColor("unformedColor", color);
    }

    public ControllerBuilderJS idleColor(String color) {
        return putColor("idleColor", color);
    }

    public ControllerBuilderJS workingColor(String color) {
        return putColor("workingColor", color);
    }

    @HideFromJS
    private ControllerBuilderJS putColor(String key, String color) {
        if (ColorUtil.parse(color) == null) throw new IllegalArgumentException("Invalid color, expected #RRGGBB: " + color);
        textures.put(key, color);
        return this;
    }

    public ControllerBuilderJS name(Object name) {
        this.nameSpec = KubeJsValues.toJson(name);
        this.name = ParserUtils.parseComponentKey(nameSpec);
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS parallelProcessingDefault(boolean parallelProcessingDefault) {
        this.parallelProcessingDefault = parallelProcessingDefault;
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS maxParallelRecipes(int maxParallelRecipes) {
        // allow negative to mean unspecified (-1); clamp to [0,100] otherwise
        if (maxParallelRecipes < 0) this.maxParallelRecipes = -1;
        else this.maxParallelRecipes = Math.min(maxParallelRecipes, 100);
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS recipeSelectionMode(String recipeSelectionMode) {
        this.recipeSelectionMode = RecipeSelectionMode.parse(recipeSelectionMode);
        return this;
    }

    @HideFromJS
    public ControllerModel build() {
        if (Ref.Controller.SINGLE.equals(type)) {
            var defined = new ArrayList<SingleMachineSlot>();
            for (PendingSlot slot : slots) {
                defined.add(new SingleMachineSlot(slot.id(), slot.type(), slot.input(), MMPortRegistry.requirePortType(slot.type()).createStorageFactory(slot.config())));
            }
            SingleMachines.define(id, defined);
        }
        var model = ControllerModel.createStyled(id, type, nameSpec, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        textures.forEach(model.config()::addProperty);
        if (geoModel != null) {
            model.config().add(GeoModelConfig.KEY, geoModel);
        }
        return model;
    }
}
