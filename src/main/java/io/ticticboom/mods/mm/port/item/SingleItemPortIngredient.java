package io.ticticboom.mods.mm.port.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

public class SingleItemPortIngredient extends BaseItemPortIngredient {

    private final Item item;
    private final ItemStack stack;
    @Getter
    private final ResourceLocation itemId;

    @Override
    protected Item indexedItem() { return item; }

    public SingleItemPortIngredient(ResourceLocation itemId, int count, CompoundTag requiredNbt, boolean nbtStrong) {
        super(count, createPredicate(itemId), requiredNbt, nbtStrong);
        this.itemId = itemId;
        item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            throw new RuntimeException(String.format("Could not find item [%s] which is required by an MM recipe", itemId));
        }
        // use a display stack with the real required count so external transfer/encode handlers
        // that read the ItemStack count (rather than the JEI badge) get the correct amount.
        stack = new ItemStack(item, count);
        if (requiredNbt != null) {
            stack.setTag(requiredNbt.copy());
        }
    }

    @Override
    public ItemStack displayItem() {
        return stack.copy();
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
        ItemStack probe = outputStack();
        List<ItemPortStorage> itemStorages = outputPorts(storages, probe);
        int remainingToInsert = count;

        for (ItemPortStorage itemStorage : itemStorages) {
            remainingToInsert = itemStorage.canInsert(probe, remainingToInsert);
            if (remainingToInsert <= 0) return true;
        }
        return remainingToInsert <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        ItemStack probe = outputStack();
        List<ItemPortStorage> itemStorages = outputPorts(storages, probe);
        
        int remainingToInsert = count;

        for (ItemPortStorage s : itemStorages) {
            if (remainingToInsert <= 0) break;
            remainingToInsert = s.insert(probe, remainingToInsert);
        }
    }

    private ItemStack outputStack() {
        ItemStack probe = new ItemStack(item, 1);
        if (requiredNbt != null) probe.setTag(requiredNbt.copy());
        return probe;
    }

    private List<ItemPortStorage> outputPorts(RecipeStorages storages, ItemStack probe) {
        List<ItemPortStorage> ports = storages.getOutputItemStorages(item);
        ports.sort((a, b) -> {
            int priority = Integer.compare(b.getPriority(), a.getPriority());
            if (priority != 0) return priority;
            int partial = Boolean.compare(b.getHandler().hasCompatiblePartialSlot(probe),
                    a.getHandler().hasCompatiblePartialSlot(probe));
            return partial != 0 ? partial : a.getStorageUid().toString().compareTo(b.getStorageUid().toString());
        });
        return ports;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ITEM, this.stack);
        // Suppress vanilla count rendering; MMRecipeCategory.draw() draws our AE2-style K/M count instead.
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        recipeSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
    }



    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        ItemStack probe = outputStack();
        List<ItemPortStorage> itemStorages = outputPorts(storages, probe);
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

            remainingToInsert = storage.canInsert(probe, remainingToInsert);

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
