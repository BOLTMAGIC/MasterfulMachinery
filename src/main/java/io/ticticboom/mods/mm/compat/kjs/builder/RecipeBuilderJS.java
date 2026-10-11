package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RecipeBuilderJS {
    private final List<JsonObject> inputs = new ArrayList<>();
    private final List<JsonObject> outputs = new ArrayList<>();
    private int ticks;
    private ResourceLocation structureId;
    private boolean parallelProcessing = false;
    private String mode;
    private JsonElement flexCost;
    private final List<ResourceLocation> extraStructureIds = new ArrayList<>();
    private final JsonArray conditions = new JsonArray();
    @Getter
    private final ResourceLocation id;

    @SuppressWarnings("removal")
    public RecipeBuilderJS(String id) {
        //noinspection removal
        this.id = new ResourceLocation(id);
    }

    public RecipeBuilderJS input(JsonObject entry) {
        inputs.add(entry);
        return this;
    }

    public RecipeBuilderJS output(JsonObject entry) {
        outputs.add(entry);
        return this;
    }

    /** "fixed" (default) or "flex": flex recipes take any item matching a flexInput and work out the output. */
    public RecipeBuilderJS mode(String mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Flex recipes: takes every kind of item matching the ingredient (e.g. {type: 'mm:item', tag: 'forge:seeds',
     * count: 1}) in one run, the ingredient's count of each, and remembers the kinds as {@code as}.
     */
    public RecipeBuilderJS flexInput(String as, JsonObject ingredient) {
        var entry = new JsonObject();
        entry.addProperty("type", "mm:input/flex");
        entry.addProperty("as", as);
        entry.add("ingredient", ingredient);
        return input(entry);
    }

    /**
     * Like {@link #flexInput(String, JsonObject, int)}; {@code chance} is the chance each kind is consumed
     * (0 keeps the items in the port).
     */
    public RecipeBuilderJS flexInput(String as, JsonObject ingredient, int maxTypes, double chance) {
        var entry = new JsonObject();
        entry.addProperty("type", "mm:input/flex");
        entry.addProperty("as", as);
        entry.addProperty("max_types", maxTypes);
        entry.addProperty("chance", chance);
        entry.add("ingredient", ingredient);
        return input(entry);
    }

    /** Flex recipes: how the other consumed inputs grow with the kinds per run: 'linear', 'once' or 'table'. */
    public RecipeBuilderJS flexCost(String mode) {
        this.flexCost = new JsonPrimitive(mode);
        return this;
    }

    /** Flex recipes: e.g. {mode: 'table', table: {'1': 1, '4': 2, '7': 3.5}}. */
    public RecipeBuilderJS flexCost(JsonObject cost) {
        this.flexCost = cost;
        return this;
    }

    /** Like {@link #flexInput(String, JsonObject)}, taking at most {@code maxTypes} kinds per run. */
    public RecipeBuilderJS flexInput(String as, JsonObject ingredient, int maxTypes) {
        var entry = new JsonObject();
        entry.addProperty("type", "mm:input/flex");
        entry.addProperty("as", as);
        entry.addProperty("max_types", maxTypes);
        entry.add("ingredient", ingredient);
        return input(entry);
    }

    /**
     * Flex recipes: outputs {@code count} of the item named by the {@code item} template for what flexInput
     * {@code from} consumed; {@code match} is a regex on the input id path ({ns}, {path}, $1.. in the template).
     */
    public RecipeBuilderJS flexOutput(String from, String match, String item, int count) {
        var entry = new JsonObject();
        entry.addProperty("type", "mm:output/flex");
        entry.addProperty("from", from);
        entry.addProperty("match", match);
        entry.addProperty("item", item);
        entry.addProperty("count", count);
        return output(entry);
    }

    /**
     * Counts the items matching the ingredient in the input ports without consuming them (at least the
     * ingredient's count, at most {@code max}), for outputs with "scale_by": {@code as}.
     */
    public RecipeBuilderJS countInput(String as, JsonObject ingredient, int max) {
        var entry = new JsonObject();
        entry.addProperty("type", "mm:input/count");
        entry.addProperty("as", as);
        entry.addProperty("max", max);
        entry.add("ingredient", ingredient);
        return input(entry);
    }

    public RecipeBuilderJS ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    public RecipeBuilderJS structureId(ResourceLocation id) {
        this.structureId = id;
        return this;
    }

    /** Lets the recipe run in more structures than structureId (e.g. every tier of a machine). */
    @SuppressWarnings("unused")
    public RecipeBuilderJS structureIds(String... ids) {
        for (String id : ids) {
            var rl = ResourceLocation.tryParse(id);
            if (rl == null) throw new IllegalArgumentException("Invalid structure id: " + id);
            if (structureId == null) structureId = rl;
            else if (!rl.equals(structureId) && !extraStructureIds.contains(rl)) extraStructureIds.add(rl);
        }
        return this;
    }

    /** Only in this dimension, e.g. "minecraft:the_nether". */
    @SuppressWarnings("unused")
    public RecipeBuilderJS dimension(String dimension) {
        return condition("dimension", "dimension", dimension);
    }

    /** Only in this biome, or a biome tag with a leading '#', e.g. "#minecraft:is_ocean". */
    @SuppressWarnings("unused")
    public RecipeBuilderJS biome(String biome) {
        return condition("biome", "biome", biome);
    }

    /** "day" or "night". */
    @SuppressWarnings("unused")
    public RecipeBuilderJS time(String time) {
        return condition("time", "time", time);
    }

    /** "rain", "thunder" or "clear". */
    @SuppressWarnings("unused")
    public RecipeBuilderJS weather(String weather) {
        return condition("weather", "weather", weather.toUpperCase(java.util.Locale.ROOT));
    }

    /** The controller must be at this Y or higher. */
    @SuppressWarnings("unused")
    public RecipeBuilderJS minY(int y) {
        return rangeCondition("height", "minY", y);
    }

    /** The controller must be at this Y or lower. */
    @SuppressWarnings("unused")
    public RecipeBuilderJS maxY(int y) {
        return rangeCondition("height", "maxY", y);
    }

    /** "powered" or "unpowered": the controller must (not) get a redstone signal. */
    @SuppressWarnings("unused")
    public RecipeBuilderJS redstone(String redstone) {
        return condition("redstone", "redstone", redstone);
    }

    /** The formed structure must be at least this tier. */
    @SuppressWarnings("unused")
    public RecipeBuilderJS minTier(double tier) {
        return rangeCondition("tier", "minTier", tier);
    }

    /** The formed structure must be at most this tier. */
    @SuppressWarnings("unused")
    public RecipeBuilderJS maxTier(double tier) {
        return rangeCondition("tier", "maxTier", tier);
    }

    private RecipeBuilderJS condition(String type, String key, String value) {
        var json = new JsonObject();
        json.addProperty("type", "mm:" + type);
        json.addProperty(key, value);
        conditions.add(json);
        return this;
    }

    /** min and max of one kind go into the same condition. */
    private RecipeBuilderJS rangeCondition(String type, String key, Number value) {
        for (var element : conditions) {
            var json = element.getAsJsonObject();
            if (("mm:" + type).equals(json.get("type").getAsString())) {
                json.addProperty(key, value);
                return this;
            }
        }
        var json = new JsonObject();
        json.addProperty("type", "mm:" + type);
        json.addProperty(key, value);
        conditions.add(json);
        return this;
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS parallelProcessing(boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
        return this;
    }

    public RecipeModel build() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("ticks", ticks);
        json.addProperty("structureId", structureId.toString());
        json.addProperty("parallelProcessing", parallelProcessing);
        if (mode != null) {
            json.addProperty("mode", mode);
        }
        if (flexCost != null) {
            json.add("flex_cost", flexCost);
        }
        var inputArr = new JsonArray();
        for (JsonObject input : inputs) {
            inputArr.add(input);
        }
        var outputArr = new JsonArray();
        for (JsonObject output : outputs) {
            outputArr.add(output);
        }
        json.add("inputs", inputArr);
        json.add("outputs", outputArr);
        if (!conditions.isEmpty()) {
            json.add("conditions", conditions);
        }
        if (!extraStructureIds.isEmpty()) {
            var ids = new JsonArray();
            extraStructureIds.forEach(id -> ids.add(id.toString()));
            json.add("structureIds", ids);
        }
        return RecipeModel.parse(json, id);
    }
}
