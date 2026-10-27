package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI shows at most 100 ingredients per slot, so a flex recipe that accepts more (e.g. every Mystical Agriculture
 * seed) is registered as several JEI recipes, one per page of its first flex input (see
 * FlexRecipeIngredientEntry#jeiPages). The machine still has one recipe; the copies only carry the page in config.
 */
public final class FlexJeiPages {

    private FlexJeiPages() {
    }

    public static List<RecipeModel> expand(RecipeModel recipe) {
        FlexRecipeIngredientEntry flex = null;
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry entry) {
                flex = entry;
                break;
            }
        }
        if (flex == null || recipe.config() == null) {
            return List.of(recipe);
        }
        var pages = flex.jeiPages();
        if (pages.size() <= 1 && pages.stream().allMatch(p -> p.tier() < 0)) {
            return List.of(recipe);
        }
        var copies = new ArrayList<RecipeModel>();
        for (int i = 0; i < pages.size(); i++) {
            var config = recipe.config().deepCopy();
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE, i);
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE_TIER, pages.get(i).tier());
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE_PART, pages.get(i).part());
            copies.add(new RecipeModel(recipe.id(), recipe.structureId(), recipe.ticks(), recipe.inputs(), recipe.outputs(),
                    new ArrayList<>(), new ArrayList<>(), recipe.conditions(), recipe.parallelProcessing(), recipe.requestOnly(), config));
        }
        return copies;
    }
}
