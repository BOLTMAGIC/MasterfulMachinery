package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

@Getter
public class ControllerBuilderJS {
    private final String id;
    private String name;
    private ResourceLocation type;
    private boolean parallelProcessingDefault = false;
    private int maxParallelRecipes = -1;
    private RecipeSelectionMode recipeSelectionMode = RecipeSelectionMode.DEFAULT;

    @HideFromJS
    public ControllerBuilderJS(String id) {
        this.id = id;
    }

    public ControllerBuilderJS type(String id) {
        var rl = ResourceLocation.tryParse(id);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + id);
        this.type = rl;
        return this;
    }

    public ControllerBuilderJS name(String name) {
        this.name = name;
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
        return ControllerModel.create(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
    }
}
