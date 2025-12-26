package io.ticticboom.mods.mm.port.item;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.ConditionalLazy;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

public class TagItemPortIngredient extends BaseItemPortIngredient {

    private final TagKey<Item> tag;
    private final ConditionalLazy<List<ItemStack>> stacks;

    public TagItemPortIngredient(ResourceLocation tagId, int count) {
        this(tagId, count, null, false);
    }

    public TagItemPortIngredient(ResourceLocation tagId, int count, CompoundTag requiredNbt, boolean nbtStrong) {
        super(count, createPredicate(tagId), requiredNbt, nbtStrong);
        this.tag = ItemTags.create(tagId);
        stacks = ConditionalLazy.create(() -> ForgeRegistries.ITEMS.tags().getTag(tag).stream().map(x -> {
            var s = new ItemStack(x, count);
            if (requiredNbt != null) s.setTag(requiredNbt.copy());
            return s;
        }).toList(),
                () -> !ForgeRegistries.ITEMS.tags().getTag(tag).isEmpty(), List.of());
    }

    private static Predicate<ItemStack> createPredicate(ResourceLocation id) {
        var key = ItemTags.create(id);
        return  i -> i.is(key);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        Ref.LOG.warn("Item Tags Ingredients will NEVER produce output, REMOVE any recipe outputs using item tags.");
        return false;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addItemStacks(stacks.get());
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("isTag", true);
        json.addProperty("WILL_NEVER_WORK", true);
        if (requiredNbt != null) {
            json.addProperty("nbt_match", nbtStrong ? "strong" : "weak");
            json.add("nbt", io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(requiredNbt));
        }
        return json;
    }
}
