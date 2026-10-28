package io.ticticboom.mods.mm.port.mekanism.heat;

import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatStack;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;

public class MekanismHeatPortIngredient implements IPortIngredient {

    private final AmountRange amount;

    public MekanismHeatPortIngredient(AmountRange amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return this.amount.max();
    }

    @Override
    public AmountRange getAmountRange() {
        return this.amount;
    }

    public int resolveAmount(RecipeStateModel state) {
        return this.amount.resolve(state);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        var inputStorages = storages.getInputStorages(MekanismHeatPortStorage.class);
        int remaining = resolveAmount(state);
        for (MekanismHeatPortStorage storage : inputStorages) {
            remaining -= storage.internalExtract(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var inputStorages = storages.getInputStorages(MekanismHeatPortStorage.class);
        int remaining = resolveAmount(state);
        for (MekanismHeatPortStorage storage : inputStorages) {
            remaining -= storage.internalExtract(remaining, false);
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        var inputStorages = storages.getInputStorages(MekanismHeatPortStorage.class);
        int remaining = resolveAmount(state);
        for (MekanismHeatPortStorage storage : inputStorages) {
            remaining -= storage.internalExtract(remaining, false);
            if (remaining <= 0) break;
        }
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        var outputStorages = storages.getOutputStoragesByPriority(MekanismHeatPortStorage.class);
        int remaining = resolveAmount(state);
        for (MekanismHeatPortStorage storage : outputStorages) {
            remaining -= storage.internalInsert(remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var outputStorages = storages.getOutputStoragesByPriority(MekanismHeatPortStorage.class);
        int remaining = resolveAmount(state);
        for (MekanismHeatPortStorage storage : outputStorages) {
            remaining -= storage.internalInsert(remaining, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.MEKANISM_HEAT, new HeatStack(amount.max()));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var inputStorages = storages.getInputStorages(MekanismHeatPortStorage.class);
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.MEK_HEAT.toString());
        amount.addToDebug(json, "amountToExtract", null);

        int remaining = amount.max();
        for (MekanismHeatPortStorage storage : inputStorages) {
            var iterJson = new JsonObject();

            var extracted = storage.internalExtract(remaining, true);
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
        var outputStorages = storages.getOutputStoragesByPriority(MekanismHeatPortStorage.class);
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.MEK_HEAT.toString());
        amount.addToDebug(json, "amountToInsert", null);

        int remaining = amount.max();
        for (MekanismHeatPortStorage storage : outputStorages) {
            var iterJson = new JsonObject();

            var inserted = storage.internalInsert(remaining, true);
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
    public PortContent display() {
        return PortContent.gauge(Component.translatable("port.mm.mekanism_heat.name"), 0xE67E22, amount.max(), 0, "J");
    }
}
