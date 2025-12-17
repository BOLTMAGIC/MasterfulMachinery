package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.model.ControllerModel;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

@Getter
public class ControllerBuilderJS {
    private final String id;
    private String name;
    private ResourceLocation type;
    private boolean parallelProcessingDefault = false;

    @HideFromJS
    public ControllerBuilderJS(String id) {
        this.id = id;
    }

    public ControllerBuilderJS type(String id) {
        //noinspection removal
        this.type = new ResourceLocation(id);
        return this;
    }

    public ControllerBuilderJS name(String name) {
        this.name = name;
        return this;
    }

    public ControllerBuilderJS parallelProcessingDefault(boolean parallelProcessingDefault) {
        this.parallelProcessingDefault = parallelProcessingDefault;
        return this;
    }

    @HideFromJS
    public ControllerModel build() {
        return ControllerModel.create(id, type, name, parallelProcessingDefault);
    }
}
