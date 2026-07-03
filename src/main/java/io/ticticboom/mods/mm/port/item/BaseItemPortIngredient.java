package io.ticticboom.mods.mm.port.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.NbtMatchUtils;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

public abstract class BaseItemPortIngredient implements IPortIngredient {

    @Getter
    protected final int count;
    protected final Predicate<ItemStack> filter;
    protected final CompoundTag requiredNbt;
    /**
     * -- GETTER --
     *  Whether NBT matching is strong (exact equality) for this ingredient.
     */
    @Getter
    protected final boolean nbtStrong;

    @SuppressWarnings("unused")
    public BaseItemPortIngredient(int count, Predicate<ItemStack> filter) {
        this(count, filter, null, false);
    }

    public BaseItemPortIngredient(int count, Predicate<ItemStack> filter, CompoundTag requiredNbt, boolean nbtStrong) {
        this.count = count;
        // wrap the provided filter to include NBT-check if requiredNbt present
        if (requiredNbt != null) {
            this.filter = filter.and(s -> {
                var tag = s.getTag();
                if (tag == null) return false;
                if (nbtStrong) {
                    return tag.equals(requiredNbt);
                } else {
                    return NbtMatchUtils.matchesWeak(requiredNbt, tag);
                }
            });
        } else {
            this.filter = filter;
        }
        this.requiredNbt = requiredNbt != null ? requiredNbt.copy() : null;
        this.nbtStrong = nbtStrong;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if(storages == null) return false;
        List<ItemPortStorage> itemStorages = storages.getInputStorages(ItemPortStorage.class);
        int remaining = count;
        for (ItemPortStorage storage : itemStorages) {
            remaining = storage.canExtract(filter, remaining);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        List<ItemPortStorage> itemStorages = storages.getInputStorages(ItemPortStorage.class);
        int remaining = count;
        for (ItemPortStorage storage : itemStorages) {
            remaining = storage.extract(filter, remaining);
        }
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        List<ItemPortStorage> itemStorages = storages.getInputStorages(ItemPortStorage.class);
        var searchedStorages = new JsonArray();
        var searchIterations = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ITEM.toString());
        json.addProperty("amountToExtract", count);

        if (requiredNbt != null) {
            json.addProperty("nbt_match", nbtStrong ? "strong" : "weak");
            json.add("nbt", NbtMatchUtils.toJson(requiredNbt));
        }

        int remaining = count;
        for (ItemPortStorage storage : itemStorages) {
            var iterJson = new JsonObject();

            remaining = storage.canExtract(this.filter, remaining);

            iterJson.addProperty("remaining", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());
            searchIterations.add(iterJson);
            searchedStorages.add(storage.getStorageUid().toString());
        }
        json.add("extractIterations", searchIterations);
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStorages);
        return json;
    }

    /**
     * Expose required NBT for callers that need to distinguish item ingredients by their NBT
     * (e.g. scheduling/round-robin based on distinct NBT variants).
     */
    public CompoundTag getRequiredNbt() {
        return requiredNbt != null ? requiredNbt.copy() : null;
    }

}
