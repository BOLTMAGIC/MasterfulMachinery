package io.ticticboom.mods.mm.port.entity;

import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredient;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityPortIngredient implements IPortIngredient {

    private final Identifier entityId;
    private final Identifier tagId;
    private final AmountRange amount;

    public EntityPortIngredient(Identifier entityId, Identifier tagId, AmountRange amount) {
        this.entityId = entityId;
        this.tagId = tagId;
        this.amount = amount;
    }

    public Identifier getEntityId() {
        return entityId;
    }

    public Identifier getTagId() {
        return tagId;
    }

    @Override
    public AmountRange getAmountRange() {
        return amount;
    }

    public int resolveAmount(RecipeStateModel state) {
        return amount.resolve(state);
    }

    private boolean matches(PortEntity entry) {
        if (entityId != null) {
            return entityId.equals(entry.type());
        }
        return EntityTypes.inTagById(entry.type(), tagId);
    }

    private EntityType<?> producedType() {
        return entityId != null ? EntityTypes.get(entityId) : EntityTypes.firstInTag(tagId);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        int remaining = resolveAmount(state);
        for (var storage : storages.getInputStorages(EntityPortStorage.class)) {
            remaining -= storage.extract(this::matches, remaining, true);
            if (remaining <= 0) {
                break;
            }
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        int remaining = resolveAmount(state);
        for (var storage : storages.getInputStorages(EntityPortStorage.class)) {
            remaining -= storage.extract(this::matches, remaining, false);
            if (remaining <= 0) {
                break;
            }
        }
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        var type = producedType();
        if (type == null) {
            return false;
        }
        int remaining = resolveAmount(state);
        for (var storage : storages.getOutputStorages(EntityPortStorage.class)) {
            remaining -= storage.produce(type, remaining, true);
            if (remaining <= 0) {
                break;
            }
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var type = producedType();
        if (type == null) {
            return;
        }
        int remaining = resolveAmount(state);
        for (var storage : storages.getOutputStorages(EntityPortStorage.class)) {
            remaining -= storage.produce(type, remaining, false);
            if (remaining <= 0) {
                break;
            }
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ENTITY,
                new EntityIngredient(entityId != null ? entityId : tagId, entityId == null, amount.max()));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ENTITY.toString());
        json.addProperty("entity", entityId == null ? null : entityId.toString());
        json.addProperty("tag", tagId == null ? null : tagId.toString());
        amount.addToDebug(json, "amountToExtract", null);

        int remaining = amount.max();
        for (var storage : storages.getInputStorages(EntityPortStorage.class)) {
            var iterJson = new JsonObject();
            var extracted = storage.extract(this::matches, remaining, true);
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
        var type = producedType();
        var searchedStoragesJson = new JsonArray();
        var searchIterationsJson = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ENTITY.toString());
        json.addProperty("entity", entityId == null ? null : entityId.toString());
        json.addProperty("tag", tagId == null ? null : tagId.toString());
        amount.addToDebug(json, "amountToInsert", null);

        int remaining = amount.max();
        for (var storage : storages.getOutputStorages(EntityPortStorage.class)) {
            var iterJson = new JsonObject();
            var inserted = type == null ? 0 : storage.produce(type, remaining, true);
            remaining -= inserted;
            iterJson.addProperty("inserted", inserted);
            iterJson.addProperty("remainingToInsert", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());
            searchIterationsJson.add(iterJson);
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.add("insertIterations", searchIterationsJson);
        json.addProperty("canRun", type != null && remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public PortContent display() {
        var type = entityId != null ? EntityTypes.get(entityId) : EntityTypes.firstInTag(tagId);
        var egg = type == null ? java.util.Optional.<net.minecraft.core.Holder<net.minecraft.world.item.Item>>empty() : SpawnEggItem.byId(type);
        if (egg.isPresent()) {
            return PortContent.item(new ItemStack(egg.get()), amount.max());
        }
        return PortContent.gauge(Component.translatable("port.mm.entity.name"), 0x7A9A5A, amount.max(), 0, "");
    }
}
