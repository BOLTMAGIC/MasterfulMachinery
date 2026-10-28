package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.util.ItemComponentUtil;
import io.ticticboom.mods.mm.util.ItemNbtUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.NbtMatchUtils;
import lombok.Getter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

public abstract class BaseItemPortIngredient implements IPortIngredient {

    protected final AmountRange count;

    protected Item indexedItem() {
        return null;
    }

    private List<ItemPortStorage> inputItemStorages(RecipeStorages storages) {
        Item item = indexedItem();
        return item == null ? storages.getInputStorages(ItemPortStorage.class) : storages.getInputItemStorages(item);
    }

    protected final Predicate<ItemStack> filter;
    protected final CompoundTag requiredNbt;
    protected final DataComponentPatch components;
    /**
     * -- GETTER --
     *  Whether NBT matching is strong (exact equality) for this ingredient.
     */
    @Getter
    protected final boolean nbtStrong;

    @SuppressWarnings("unused")
    public BaseItemPortIngredient(AmountRange count, Predicate<ItemStack> filter) {
        this(count, filter, null, false, DataComponentPatch.EMPTY);
    }

    public BaseItemPortIngredient(AmountRange count, Predicate<ItemStack> filter, CompoundTag requiredNbt, boolean nbtStrong, DataComponentPatch components) {
        this.count = count;
        if (!components.isEmpty()) {
            filter = filter.and(s -> ItemComponentUtil.matches(s, components));
        }
        // wrap the provided filter to include NBT-check if requiredNbt present
        if (requiredNbt != null) {
            this.filter = filter.and(s -> {
                var tag = ItemNbtUtil.getTag(s);
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
        this.components = components;
    }

    @Override
    public AmountRange getAmountRange() {
        return count;
    }

    public int getCount() {
        return count.max();
    }

    /** Every item this ingredient accepts, for JEI (flex recipes list one output per accepted item). */
    public List<ItemStack> exampleStacks() {
        var content = display();
        return content == null || content.item() == null || content.item().isEmpty() ? List.of() : List.of(content.item().copy());
    }

    /** Whether a stack is accepted by this ingredient (item/tag and components), ignoring its count. */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && filter.test(stack);
    }

    public int resolveCount(RecipeStateModel state) {
        return count.resolve(state);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if(storages == null) return false;
        List<ItemPortStorage> itemStorages = inputItemStorages(storages);
        int remaining = resolveCount(state);
        for (ItemPortStorage storage : itemStorages) {
            remaining = indexedItem() == null ? storage.canExtract(filter, remaining)
                    : storage.canExtract(indexedItem(), filter, remaining);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        List<ItemPortStorage> itemStorages = inputItemStorages(storages);
        int remaining = resolveCount(state);
        for (ItemPortStorage storage : itemStorages) {
            remaining = indexedItem() == null ? storage.extract(filter, remaining)
                    : storage.extract(indexedItem(), filter, remaining);
        }
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        List<ItemPortStorage> itemStorages = inputItemStorages(storages);
        var searchedStorages = new JsonArray();
        var searchIterations = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ITEM.toString());
        count.addToDebug(json, "amountToExtract", null);

        if (requiredNbt != null) {
            json.addProperty("nbt_match", nbtStrong ? "strong" : "weak");
            json.add("nbt", NbtMatchUtils.toJson(requiredNbt));
        }

        int remaining = count.max();
        for (ItemPortStorage storage : itemStorages) {
            var iterJson = new JsonObject();

            remaining = indexedItem() == null ? storage.canExtract(this.filter, remaining)
                    : storage.canExtract(indexedItem(), this.filter, remaining);

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
