package io.ticticboom.mods.mm.port.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

public class SingleItemPortIngredient extends BaseItemPortIngredient {

    private final Item item;
    private final ItemStack stack;

    public SingleItemPortIngredient(ResourceLocation itemId, int count) {
        this(itemId, count, null, false);
    }

    public SingleItemPortIngredient(ResourceLocation itemId, int count, CompoundTag requiredNbt, boolean nbtStrong) {
        super(count, createPredicate(itemId), requiredNbt, nbtStrong);
        item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            throw new RuntimeException(String.format("Could not find item [%s] which is required by an MM recipe", itemId));
        }
        stack = new ItemStack(item, count);
        if (requiredNbt != null) {
            stack.setTag(requiredNbt.copy());
        }
    }

    private static Predicate<ItemStack> createPredicate(ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new RuntimeException(String.format("Could not find item [%s] which is required by an MM recipe", id));
        }
        return c -> c.is(item);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        List<ItemPortStorage> itemStorages = storages.getOutputStorages(ItemPortStorage.class);
        // sort storages by priority desc so high prio filled first
        itemStorages.sort(Comparator.comparingInt(ItemPortStorage::getPriority).reversed()
                .thenComparing(s -> s.getStorageUid().toString()));
        int remainingToInsert = count;
        for (ItemPortStorage itemStorage : itemStorages) {
            if (this.requiredNbt != null) {
                ItemStack probe = new ItemStack(item, 1);
                probe.setTag(this.requiredNbt.copy());
                remainingToInsert = itemStorage.canInsert(probe, remainingToInsert);
            } else {
                remainingToInsert = itemStorage.canInsert(item, remainingToInsert);
            }
        }
        return remainingToInsert <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        List<ItemPortStorage> itemStorages = storages.getOutputStorages(ItemPortStorage.class);
        // group storages by priority descending
        var grouped = new java.util.TreeMap<Integer, List<ItemPortStorage>>(java.util.Collections.reverseOrder());
        for (ItemPortStorage s : itemStorages) {
            grouped.computeIfAbsent(s.getPriority(), k -> new java.util.ArrayList<>()).add(s);
        }

        int remainingToInsert = count;
        for (var entry : grouped.entrySet()) {
            var group = entry.getValue();
            // compute total available in this priority group
            int totalAvailable = 0;
            for (ItemPortStorage s : group) {
                if (this.requiredNbt != null) {
                    ItemStack probe = new ItemStack(item, 1);
                    probe.setTag(this.requiredNbt.copy());
                    totalAvailable += s.canInsert(probe, Integer.MAX_VALUE);
                } else {
                    totalAvailable += s.canInsert(item, Integer.MAX_VALUE);
                }
            }
            if (totalAvailable <= 0) continue;

            // try to fill this priority group as much as possible
            // sort group by available capacity desc, then uid to be deterministic
            group.sort((a, b) -> {
                int avA = a.canInsert(item, Integer.MAX_VALUE);
                int avB = b.canInsert(item, Integer.MAX_VALUE);
                if (avA != avB) return Integer.compare(avB, avA);
                return a.getStorageUid().toString().compareTo(b.getStorageUid().toString());
            });
            for (ItemPortStorage s : group) {
                if (remainingToInsert <= 0) break;
                int before = remainingToInsert;
                if (this.requiredNbt != null) {
                    ItemStack outStack = new ItemStack(item, remainingToInsert);
                    outStack.setTag(this.requiredNbt.copy());
                    remainingToInsert = s.insert(outStack, remainingToInsert);
                } else {
                    remainingToInsert = s.insert(item, remainingToInsert);
                }
                int moved = before - remainingToInsert;
            }

            if (remainingToInsert <= 0) break;
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ITEM, this.stack);
    }



    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        List<ItemPortStorage> itemStorages = storages.getOutputStorages(ItemPortStorage.class);
        itemStorages.sort(Comparator.comparingInt(ItemPortStorage::getPriority).reversed()
                .thenComparing(s -> s.getStorageUid().toString()));
        var searchedStorages = new JsonArray();
        var searchIterations = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.ITEM.toString());
        json.addProperty("amountToInsert", count);

        if (requiredNbt != null) {
            json.addProperty("nbt_match", nbtStrong ? "strong" : "weak");
            json.add("nbt", io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(requiredNbt));
        }

        int remainingToInsert = count;
        for (ItemPortStorage storage : itemStorages) {
            var iterJson = new JsonObject();

            remainingToInsert = storage.canInsert(item, remainingToInsert);

            iterJson.addProperty("remaining", remainingToInsert);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());
            searchIterations.add(iterJson);
            searchedStorages.add(storage.getStorageUid().toString());
        }

        json.add("insertIterations", searchIterations);
        json.addProperty("canRun", remainingToInsert <= 0);
        json.add("searchedStorages", searchedStorages);
        return json;
    }
}
