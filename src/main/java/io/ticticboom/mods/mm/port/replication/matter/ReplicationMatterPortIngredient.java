package io.ticticboom.mods.mm.port.replication.matter;

import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.port.PortContent;
import com.buuz135.replication.api.IMatterType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredient;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientNames;

public class ReplicationMatterPortIngredient implements IPortIngredient {

    private final Identifier matterId;
    private final AmountRange amount;

    public ReplicationMatterPortIngredient(Identifier matterId, AmountRange amount) {
        this.matterId = matterId;
        this.amount = amount;
    }

    public Identifier getMatterId() {
        return matterId;
    }

    @Override
    public AmountRange getAmountRange() {
        return this.amount;
    }

    public int resolveAmount(RecipeStateModel state) {
        return this.amount.resolve(state);
    }

    private IMatterType type() {
        return MatterTypes.get(matterId);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        var type = type();
        if (type == null) return false;
        int remaining = resolveAmount(state);
        for (var storage : storages.getInputStorages(ReplicationMatterPortStorage.class)) {
            remaining -= storage.internalExtract(type, remaining, true);
            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var type = type();
        if (type == null) return;
        int remaining = resolveAmount(state);
        for (var storage : storages.getInputStorages(ReplicationMatterPortStorage.class)) {
            remaining -= storage.internalExtract(type, remaining, false);
            if (remaining <= 0) break;
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        var type = type();
        if (type == null) return false;
        int remaining = resolveAmount(state);
        for (var storage : storages.getOutputStorages(ReplicationMatterPortStorage.class)) {
            remaining -= storage.internalInsert(type, remaining, true);
            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var type = type();
        if (type == null) return;
        int remaining = resolveAmount(state);
        for (var storage : storages.getOutputStorages(ReplicationMatterPortStorage.class)) {
            remaining -= storage.internalInsert(type, remaining, false);
            if (remaining <= 0) break;
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.REPLICATION_MATTER,
                new MatterIngredient(matterId, amount.max(), MatterTypes.colorOf(type())));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var type = type();
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.REPLICATION_MATTER.toString());
        json.addProperty("matter", matterId.toString());
        amount.addToDebug(json, "amountToExtract", null);

        int remaining = amount.max();
        for (var storage : storages.getInputStorages(ReplicationMatterPortStorage.class)) {
            var iterJson = new JsonObject();
            var extracted = type == null ? 0 : storage.internalExtract(type, remaining, true);
            remaining -= extracted;
            iterJson.addProperty("extracted", extracted);
            iterJson.addProperty("remainingToExtract", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());
            searchIterationsJson.add(iterJson);
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.add("extractIterations", searchIterationsJson);
        json.addProperty("canRun", type != null && remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        var type = type();
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.REPLICATION_MATTER.toString());
        json.addProperty("matter", matterId.toString());
        amount.addToDebug(json, "amountToInsert", null);

        int remaining = amount.max();
        for (var storage : storages.getOutputStorages(ReplicationMatterPortStorage.class)) {
            var iterJson = new JsonObject();
            var inserted = type == null ? 0 : storage.internalInsert(type, remaining, true);
            remaining -= inserted;
            iterJson.addProperty("inserted", inserted);
            iterJson.addProperty("remainingToInsert", remaining);
            searchIterationsJson.add(iterJson);
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.add("insertIterations", searchIterationsJson);
        json.addProperty("canRun", type != null && remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public net.minecraft.network.chat.Component displayName() {
        return net.minecraft.network.chat.Component.translatable("gui.mm.controller.recipe_output.matter",
                MatterIngredientNames.displayName(matterId));
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(Component.literal(matterId.getPath()), MatterTypes.colorOf(type()), amount.max(), 0, "");
    }
}
