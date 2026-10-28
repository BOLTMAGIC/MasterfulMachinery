package io.ticticboom.mods.mm.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;


public record ControllerModel(
        String id,
        Identifier type,
        String name,
        Supplier<Component> displayNameSource,
        boolean parallelProcessingDefault,
        int maxParallelRecipes,
        RecipeSelectionMode recipeSelectionMode,
        JsonObject config
) {

    public Component displayName() {
        return displayNameSource.get();
    }

    public Identifier overlayTexture() {
        return ParserUtils.parseOptionalId(config, "overlay");
    }

    public Identifier baseTexture() {
        return ParserUtils.parseOptionalId(config, "texture");
    }

    public String screenColor(String state) {
        var key = state + "Color";
        if (config == null || !config.has(key) || !config.get(key).isJsonPrimitive()) return null;
        return config.get(key).getAsString();
    }

    public Identifier workingSound() {
        return configId("workingSound");
    }

    public int workingSoundInterval() {
        if (config == null || !config.has("workingSoundInterval") || !config.get("workingSoundInterval").isJsonPrimitive()) return 40;
        return Math.max(1, config.get("workingSoundInterval").getAsInt());
    }

    public Identifier workingParticle() {
        return configId("workingParticle");
    }

    private Identifier configId(String key) {
        if (config == null || !config.has(key) || !config.get(key).isJsonPrimitive()) return null;
        return Identifier.tryParse(config.get(key).getAsString());
    }

    public Identifier customModel() {
        return ParserUtils.parseOptionalId(config, "model");
    }

    public Identifier customModel(String state) {
        var specific = ParserUtils.parseOptionalId(config, state + "Model");
        return specific != null ? specific : customModel();
    }

    public Identifier overlayTexture(String state) {
        var specific = ParserUtils.parseOptionalId(config, state + "Overlay");
        return specific != null ? specific : overlayTexture();
    }

    private static Supplier<Component> literalOf(String name) {
        var literal = Component.literal(name);
        return () -> literal;
    }

    public static ControllerModel parse(JsonObject json) {
        var id = json.get("id").getAsString();
        // "name" accepts a plain string or a { "translation": "key" } object.
        var name = ParserUtils.parseComponentKey(json.get("name"));
        var displayName = ParserUtils.parseNameSupplier(json.get("name"));
        var type = ParserUtils.parseId(json, "type");
        var parallelProcessingDefault = json.has("parallelProcessingDefault") && json.get("parallelProcessingDefault").getAsBoolean();
        var maxParallelRecipes = json.has("maxParallelRecipes") ? json.get("maxParallelRecipes").getAsInt() : -1; // -1 => use global default
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        var recipeSelectionMode = json.has("recipeSelectionMode")
                ? RecipeSelectionMode.parse(json.get("recipeSelectionMode").getAsString())
                : RecipeSelectionMode.DEFAULT;
        return new ControllerModel(id, type, name, displayName, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode, json);
    }

    public static ControllerModel create(String id, Identifier type, String name) {
        JsonObject json = paramsToJson(id, type, name);
        return new ControllerModel(id, type, name, literalOf(name), false, -1, RecipeSelectionMode.DEFAULT, json);
    }

    public static ControllerModel create(String id, Identifier type, String name, boolean parallelProcessingDefault) {
        JsonObject json = paramsToJson(id, type, name, parallelProcessingDefault, -1);
        return new ControllerModel(id, type, name, literalOf(name), parallelProcessingDefault, -1, RecipeSelectionMode.DEFAULT, json);
    }

    public static ControllerModel create(String id, Identifier type, String name, boolean parallelProcessingDefault, int maxParallelRecipes) {
        return create(id, type, name, parallelProcessingDefault, maxParallelRecipes, RecipeSelectionMode.DEFAULT);
    }

    public static ControllerModel create(String id, Identifier type, String name, boolean parallelProcessingDefault, int maxParallelRecipes, RecipeSelectionMode recipeSelectionMode) {
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        if (recipeSelectionMode == null) recipeSelectionMode = RecipeSelectionMode.DEFAULT;
        JsonObject json = paramsToJson(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        return new ControllerModel(id, type, name, literalOf(name), parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode, json);
    }

    public static ControllerModel createStyled(String id, Identifier type, JsonElement nameSpec, boolean parallelProcessingDefault, int maxParallelRecipes, RecipeSelectionMode recipeSelectionMode) {
        if (nameSpec == null || nameSpec.isJsonNull()) {
            return create(id, type, null, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        }
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        if (recipeSelectionMode == null) recipeSelectionMode = RecipeSelectionMode.DEFAULT;
        var name = ParserUtils.parseComponentKey(nameSpec);
        JsonObject json = paramsToJson(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        return new ControllerModel(id, type, name, ParserUtils.parseNameSupplier(nameSpec), parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode, json);
    }

    public static JsonObject paramsToJson(String id, Identifier type, String name) {
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.toString());
        json.addProperty("name", name);
        json.addProperty("parallelProcessingDefault", false);
        json.addProperty("maxParallelRecipes", -1);
        json.addProperty("recipeSelectionMode", RecipeSelectionMode.DEFAULT.serializedName());
        return json;
    }

    public static JsonObject paramsToJson(String id, Identifier type, String name, boolean parallelProcessingDefault, int maxParallelRecipes) {
        return paramsToJson(id, type, name, parallelProcessingDefault, maxParallelRecipes, RecipeSelectionMode.DEFAULT);
    }

    public static JsonObject paramsToJson(String id, Identifier type, String name, boolean parallelProcessingDefault, int maxParallelRecipes, RecipeSelectionMode recipeSelectionMode) {
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        if (recipeSelectionMode == null) recipeSelectionMode = RecipeSelectionMode.DEFAULT;
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.toString());
        json.addProperty("name", name);
        json.addProperty("parallelProcessingDefault", parallelProcessingDefault);
        json.addProperty("maxParallelRecipes", maxParallelRecipes);
        json.addProperty("recipeSelectionMode", recipeSelectionMode.serializedName());
        return json;
    }

    private static int clampMaxParallelRecipesMarker(int v) {
        // -1 => use global default; otherwise clamp to [0,100]
        if (v == -1) return -1;
        if (v < 0) return -1; // treat other negatives as unspecified
        return Math.min(v, 100);
    }
}
