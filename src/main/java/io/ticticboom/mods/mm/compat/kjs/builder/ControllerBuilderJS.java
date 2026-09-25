package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.config.MMClientConfig;
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
    private String unformedColor;
    private String idleColor;
    private String workingColor;
    private String workingSound;
    private String workingParticle;

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

    /** Screen color while the multiblock is not built, as "#RRGGBB"; overrides the client config. */
    @SuppressWarnings("unused")
    public ControllerBuilderJS unformedColor(String color) {
        this.unformedColor = checkColor(color);
        return this;
    }

    /** Screen color while the multiblock is built but idle, as "#RRGGBB"; overrides the client config. */
    @SuppressWarnings("unused")
    public ControllerBuilderJS idleColor(String color) {
        this.idleColor = checkColor(color);
        return this;
    }

    /** Screen color while a recipe is running, as "#RRGGBB"; overrides the client config. */
    @SuppressWarnings("unused")
    public ControllerBuilderJS workingColor(String color) {
        this.workingColor = checkColor(color);
        return this;
    }

    /** Sound played now and then while a recipe is running, e.g. "minecraft:block.blastfurnace.fire_crackle". */
    @SuppressWarnings("unused")
    public ControllerBuilderJS workingSound(String sound) {
        this.workingSound = checkId(sound);
        return this;
    }

    /** Particle shown on the controller's front while a recipe is running, e.g. "minecraft:smoke" (particles without options only). */
    @SuppressWarnings("unused")
    public ControllerBuilderJS workingParticle(String particle) {
        this.workingParticle = checkId(particle);
        return this;
    }

    private static String checkId(String id) {
        if (ResourceLocation.tryParse(id) == null) throw new IllegalArgumentException("Invalid id: " + id);
        return id;
    }

    private static String checkColor(String color) {
        if (MMClientConfig.parseColor(color) == null) throw new IllegalArgumentException("Invalid color, expected #RRGGBB: " + color);
        return color;
    }

    @HideFromJS
    public ControllerModel build() {
        var model = ControllerModel.create(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        if (unformedColor != null) model.config().addProperty("unformedColor", unformedColor);
        if (idleColor != null) model.config().addProperty("idleColor", idleColor);
        if (workingColor != null) model.config().addProperty("workingColor", workingColor);
        if (workingSound != null) model.config().addProperty("workingSound", workingSound);
        if (workingParticle != null) model.config().addProperty("workingParticle", workingParticle);
        return model;
    }
}
