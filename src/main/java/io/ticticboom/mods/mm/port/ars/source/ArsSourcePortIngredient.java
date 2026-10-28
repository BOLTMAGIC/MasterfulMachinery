package io.ticticboom.mods.mm.port.ars.source;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceStack;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ArsSourcePortIngredient implements IPortIngredient {

    private final AmountRange source;

    public ArsSourcePortIngredient(AmountRange source) {
        this.source = source;
    }

    private int amount(RecipeStateModel state) {
        return state == null ? source.max() : source.resolve(state);
    }

    @Override
    public AmountRange getAmountRange() {
        return source;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        var remaining = amount(state);
        for (ArsSourcePortStorage input : storages.getInputStorages(ArsSourcePortStorage.class)) {
            remaining -= input.extract(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var remaining = amount(state);
        for (ArsSourcePortStorage input : storages.getInputStorages(ArsSourcePortStorage.class)) {
            remaining -= input.extract(remaining, false);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        var remaining = amount(state);
        for (ArsSourcePortStorage output : storages.getOutputStorages(ArsSourcePortStorage.class)) {
            remaining -= output.receive(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var remaining = amount(state);
        for (ArsSourcePortStorage output : storages.getOutputStorages(ArsSourcePortStorage.class)) {
            remaining -= output.receive(remaining, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ARS_SOURCE, new ArsSourceStack(source.min(), source.max()));
    }

    @Override
    public Component displayName() {
        return Component.translatable("port.mm.ars_source.name");
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ARS_SOURCE.toString());
        source.addToDebug(json, "amountToExtract", null);
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ARS_SOURCE.toString());
        source.addToDebug(json, "amountToInsert", null);
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(Component.translatable("port.mm.ars_source.name"), ArsSourcePortStorage.FILL_COLOR, source.max(), 0, "Source");
    }
}
