package io.ticticboom.mods.mm.recipe;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import io.ticticboom.mods.mm.recipe.condition.biome.BiomeRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.height.HeightRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.redstone.RedstoneRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.tier.TierRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.time.TimeRecipeCondition;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.IRecipeConditionParser;
import io.ticticboom.mods.mm.recipe.condition.dimension.DimensionRecipeConditionParser;
import io.ticticboom.mods.mm.recipe.condition.weather.WeatherRecipeConditionParser;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntryParser;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntryParser;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntryParser;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntryParser;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineRecipeManager extends SimpleJsonResourceReloadListener<JsonElement> {

    public MachineRecipeManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("mm/processes"));
        init();
    }

    public static final Map<Identifier, RecipeModel> RECIPES = new HashMap<>();
    public static final Map<String, Map<Identifier, RecipeModel>> RECIPES_BY_STRUCTURE = new HashMap<>();
    public static int RECIPE_VERSION = 0;
    public static final Map<Identifier, IRecipeIngredientEntryParser> ENTRY_INGREDIENT_PARSERS = new HashMap<>();
    public static final Map<Identifier, IRecipeOutputEntryParser> ENTRY_OUTPUT_PARSERS = new HashMap<>();
    public static final Map<Identifier, IRecipeConditionParser> CONDITION_PARSERS = new HashMap<>();

    public static void init() {
        CONDITION_PARSERS.clear();
        ENTRY_OUTPUT_PARSERS.clear();
        ENTRY_INGREDIENT_PARSERS.clear();
        ENTRY_INGREDIENT_PARSERS.put(Ref.RecipeEntries.CONSUME_INPUT, new ConsumeRecipeIngredientEntryParser());
        ENTRY_INGREDIENT_PARSERS.put(Ref.RecipeEntries.FLEX_INPUT, new io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntryParser());
        ENTRY_INGREDIENT_PARSERS.put(Ref.RecipeEntries.COUNT_INPUT, new io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntryParser());
        ENTRY_INGREDIENT_PARSERS.put(Ref.RecipeEntries.FLEX_MATCH_INPUT, new io.ticticboom.mods.mm.recipe.input.flex.FlexMatchIngredientEntryParser());
        ENTRY_OUTPUT_PARSERS.put(Ref.RecipeEntries.SIMPLE_OUTPUT, new SimpleRecipeOutputEntryParser());
        ENTRY_OUTPUT_PARSERS.put(Ref.RecipeEntries.WEIGHTED_OUTPUT, new io.ticticboom.mods.mm.recipe.output.weighted.WeightedRecipeOutputEntryParser());
        ENTRY_OUTPUT_PARSERS.put(Ref.RecipeEntries.FLEX_OUTPUT, new io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntryParser());
        CONDITION_PARSERS.put(Ref.RecipeConditions.DIMENSION, new DimensionRecipeConditionParser());
        CONDITION_PARSERS.put(Ref.RecipeConditions.WEATHER, new WeatherRecipeConditionParser());
        CONDITION_PARSERS.put(Ref.RecipeConditions.BIOME, BiomeRecipeCondition::parse);
        CONDITION_PARSERS.put(Ref.RecipeConditions.TIME, TimeRecipeCondition::parse);
        CONDITION_PARSERS.put(Ref.RecipeConditions.HEIGHT, HeightRecipeCondition::parse);
        CONDITION_PARSERS.put(Ref.RecipeConditions.REDSTONE, RedstoneRecipeCondition::parse);
        CONDITION_PARSERS.put(Ref.RecipeConditions.TIER, TierRecipeCondition::parse);
    }

    public static IRecipeIngredientEntry parseIngredientEntry(JsonObject json) {
        var typeId = ParserUtils.parseId(json, "type");
        return ENTRY_INGREDIENT_PARSERS.get(typeId).parse(json);
    }

    public static IRecipeOutputEntry parseOutputEntry(JsonObject json) {
        var typeId = ParserUtils.parseId(json, "type");
        return ENTRY_OUTPUT_PARSERS.get(typeId).parse(json);
    }

    public static IRecipeCondition parseCondition(JsonObject json) {
        var typeId = ParserUtils.parseId(json, "type");
        return CONDITION_PARSERS.get(typeId).parse(json);
    }

    public static List<RecipeModel> getRecipeForStructureIds(List<Identifier> structureIds) {
        return RECIPES.values().stream().filter(x -> x.allStructureIds().stream().anyMatch(structureIds::contains)).toList();
    }

    public static Collection<RecipeModel> getRecipesByStrucutreId(Identifier id) {
        if(RECIPES_BY_STRUCTURE.containsKey(id.toString())) {
            return RECIPES_BY_STRUCTURE.get(id.toString()).values();
        }
        return List.of();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        profilerFiller.push("MM Machine Recipe Processes");
        recieveRecipes(jsons);
        profilerFiller.pop();
    }

    /** Goes up on every recipe (re)load, so caches of recipes can tell they are stale. */
    public static int generation = 0;

    public static void recieveRecipes(Map<Identifier, JsonElement> jsons) {
        RECIPES.clear();
        generation++;
        Ref.LCTX.reset("MM Machine Recipe Processes");
        try {
            // Each recipe is parsed independently. A single bad one used to abort the whole load
            // and rethrow, which crashed world creation -- and the most likely cause of a bad
            // recipe is a pack shipping content for an integration mod that is not installed,
            // which should degrade rather than make the world unloadable.
            int skipped = 0;
            for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
                Identifier id = entry.getKey();
                Ref.LCTX.push("Loading Recipe: " + id);
                try {
                    RECIPES.put(id, RecipeModel.parse(entry.getValue().getAsJsonObject(), id));
                } catch (Exception e) {
                    skipped++;
                    Ref.LOG.error("Skipping recipe {}: {}", id, e.getMessage());
                }
                Ref.LCTX.pop();
            }
            if (skipped > 0) {
                Ref.LOG.error("{} MM recipe(s) failed to load and were skipped. The machines they "
                        + "belong to will not have those processes available.", skipped);
            }
            if (MMInteropManager.KUBEJS.isPresent()) {
                Ref.LCTX.push("Loading KubeJS Recipes ");
                for (RecipeModel model : MMInteropManager.KUBEJS.get().postCreateRecipes()) {
                    Ref.LCTX.push("Loading Recipe: " + model.id());
                    RECIPES.put(model.id(), model);
                    Ref.LCTX.pop();
                }
                Ref.LCTX.pop();
            }
        } catch (Exception e) {
            Ref.LCTX.doThrow(e);
        }
        cacheRecipesByStructure();
    }

    private static void cacheRecipesByStructure() {
        RECIPES_BY_STRUCTURE.clear();
        for (RecipeModel model : RECIPES.values()) {
            for (Identifier structureId : model.allStructureIds()) {
                RECIPES_BY_STRUCTURE.computeIfAbsent(structureId.toString(), x -> new HashMap<>()).put(model.id(), model);
            }
        }
        RECIPE_VERSION++;
    }
}
