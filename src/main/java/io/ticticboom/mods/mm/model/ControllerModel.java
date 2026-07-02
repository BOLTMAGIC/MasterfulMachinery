package io.ticticboom.mods.mm.model;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;


public record ControllerModel(
        String id,
        ResourceLocation type,
        String name,
        boolean parallelProcessingDefault,
        int maxParallelRecipes,
        RecipeSelectionMode recipeSelectionMode,
        JsonObject config
) {
    public static ControllerModel parse(JsonObject json) {
        var id = json.get("id").getAsString();
        var name = json.get("name").getAsString();
        var type = ParserUtils.parseId(json, "type");
        var parallelProcessingDefault = json.has("parallelProcessingDefault") && json.get("parallelProcessingDefault").getAsBoolean();
        var maxParallelRecipes = json.has("maxParallelRecipes") ? json.get("maxParallelRecipes").getAsInt() : -1; // -1 => use global default
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        var recipeSelectionMode = json.has("recipeSelectionMode")
                ? RecipeSelectionMode.parse(json.get("recipeSelectionMode").getAsString())
                : RecipeSelectionMode.DEFAULT;
        return new ControllerModel(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode, json);
    }

    public static ControllerModel create(String id, ResourceLocation type, String name) {
        JsonObject json = paramsToJson(id, type, name);
        return new ControllerModel(id, type, name, false, -1, RecipeSelectionMode.DEFAULT, json);
    }

    public static ControllerModel create(String id, ResourceLocation type, String name, boolean parallelProcessingDefault) {
        JsonObject json = paramsToJson(id, type, name, parallelProcessingDefault, -1);
        return new ControllerModel(id, type, name, parallelProcessingDefault, -1, RecipeSelectionMode.DEFAULT, json);
    }

    public static ControllerModel create(String id, ResourceLocation type, String name, boolean parallelProcessingDefault, int maxParallelRecipes) {
        return create(id, type, name, parallelProcessingDefault, maxParallelRecipes, RecipeSelectionMode.DEFAULT);
    }

    public static ControllerModel create(String id, ResourceLocation type, String name, boolean parallelProcessingDefault, int maxParallelRecipes, RecipeSelectionMode recipeSelectionMode) {
        maxParallelRecipes = clampMaxParallelRecipesMarker(maxParallelRecipes);
        if (recipeSelectionMode == null) recipeSelectionMode = RecipeSelectionMode.DEFAULT;
        JsonObject json = paramsToJson(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        return new ControllerModel(id, type, name, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode, json);
    }

    public static JsonObject paramsToJson(String id, ResourceLocation type, String name) {
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.toString());
        json.addProperty("name", name);
        json.addProperty("parallelProcessingDefault", false);
        json.addProperty("maxParallelRecipes", -1);
        json.addProperty("recipeSelectionMode", RecipeSelectionMode.DEFAULT.serializedName());
        return json;
    }

    public static JsonObject paramsToJson(String id, ResourceLocation type, String name, boolean parallelProcessingDefault, int maxParallelRecipes) {
        return paramsToJson(id, type, name, parallelProcessingDefault, maxParallelRecipes, RecipeSelectionMode.DEFAULT);
    }

    public static JsonObject paramsToJson(String id, ResourceLocation type, String name, boolean parallelProcessingDefault, int maxParallelRecipes, RecipeSelectionMode recipeSelectionMode) {
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
