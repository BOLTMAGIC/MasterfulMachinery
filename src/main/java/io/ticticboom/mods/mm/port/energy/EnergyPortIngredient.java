package io.ticticboom.mods.mm.port.energy;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyStack;
import io.ticticboom.mods.mm.port.ITickSpreadIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.LongAmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

public class EnergyPortIngredient implements ITickSpreadIngredient {

    private final LongAmountRange amount;

    public EnergyPortIngredient(LongAmountRange amount) {
        this.amount = amount;
    }

    @Override
    public IPortIngredient scaled(double factor) {
        return new EnergyPortIngredient(amount.scaled(factor));
    }

    @Override
    public PortContent display() {
        return PortContent.energy(amount.max(), 0);
    }

    public long getAmount() {
        return this.amount.max();
    }

    @Override
    public long resolveAmount(RecipeStateModel state) {
        return this.amount.resolve(state);
    }

    @Override
    public long extractFromInputs(RecipeStorages storages, long amount, boolean simulate) {
        long extracted = 0;
        for (EnergyPortStorage storage : storages.getInputStorages(EnergyPortStorage.class)) {
            extracted += storage.internalExtract(amount - extracted, simulate);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        long remaining = resolveAmount(state);
        for (EnergyPortStorage storage : storages.getInputStorages(EnergyPortStorage.class)) {
            remaining -= storage.internalExtract(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        long remaining = resolveAmount(state);
        for (EnergyPortStorage storage : storages.getInputStorages(EnergyPortStorage.class)) {
            remaining -= storage.internalExtract(remaining, false);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        long remaining = resolveAmount(state);
        for (EnergyPortStorage storage : storages.getInputStorages(EnergyPortStorage.class)) {
            remaining -= storage.internalExtract(remaining, false);
            if (remaining <= 0) break;
        }
    }

    @Override
    public boolean canSimulateOutput() {
        return true;
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        long remaining = resolveAmount(state);
        for (EnergyPortStorage storage : storages.getOutputStoragesByPriority(EnergyPortStorage.class)) {
            remaining -= storage.internalInsert(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        long remaining = resolveAmount(state);
        for (EnergyPortStorage storage : storages.getOutputStoragesByPriority(EnergyPortStorage.class)) {
            remaining -= storage.internalInsert(remaining, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ENERGY, new EnergyStack(amount.min(), amount.max()));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ENERGY.toString());
        json.addProperty("amountToExtract", amount.max());
        long remaining = amount.max();
        for (EnergyPortStorage storage : storages.getInputStorages(EnergyPortStorage.class)) {
            var iterJson = new JsonObject();
            long extracted = storage.internalExtract(remaining, true);
            remaining -= extracted;
            iterJson.addProperty("extracted", extracted);
            iterJson.addProperty("remainingToExtract", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());
            searchIterationsJson.add(iterJson);
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.add("extractIterations", searchIterationsJson);
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ENERGY.toString());
        json.addProperty("amountToInsert", amount.max());
        long remaining = amount.max();
        for (EnergyPortStorage storage : storages.getOutputStoragesByPriority(EnergyPortStorage.class)) {
            var iterJson = new JsonObject();
            long inserted = storage.internalInsert(remaining, true);
            remaining -= inserted;
            iterJson.addProperty("inserted", inserted);
            iterJson.addProperty("remainingToInsert", remaining);
            searchIterationsJson.add(iterJson);
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.add("insertIterations", searchIterationsJson);
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public net.minecraft.network.chat.Component displayName() {
        return net.minecraft.network.chat.Component.translatable("port.mm.energy.name");
    }
}
