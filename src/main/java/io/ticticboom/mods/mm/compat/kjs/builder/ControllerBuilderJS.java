package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
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
    private Integer workingSoundInterval;
    private JsonElement geoModel;

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

    /** Ticks between two plays of the working sound (default 40, two seconds); match it to the sound's length. */
    @SuppressWarnings("unused")
    public ControllerBuilderJS workingSoundInterval(int ticks) {
        if (ticks < 1) throw new IllegalArgumentException("workingSoundInterval must be at least 1 tick: " + ticks);
        this.workingSoundInterval = ticks;
        return this;
    }

    /** Particle shown on the controller's front while a recipe is running, e.g. "minecraft:smoke" (particles without options only). */
    @SuppressWarnings("unused")
    public ControllerBuilderJS workingParticle(String particle) {
        this.workingParticle = checkId(particle);
        return this;
    }

    /**
     * GeckoLib model drawn while the machine is formed (needs GeckoLib), e.g.
     * {model: 'mm:geo/core.geo.json', texture: 'mm:textures/block/core.png', animations: 'mm:animations/core.animation.json',
     *  idle: 'animation.core.idle', working: 'animation.core.working', offset: [0, 1, 2], scale: 1, rotate: true, glow: false}.
     * A structure's geoModel replaces this one.
     */
    @SuppressWarnings("unused")
    public ControllerBuilderJS geoModel(JsonObject geoModel) {
        this.geoModel = geoModel;
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
        if (workingSoundInterval != null) model.config().addProperty("workingSoundInterval", workingSoundInterval);
        if (geoModel != null) model.config().add(GeoModelConfig.KEY, geoModel);
        return model;
    }
}
