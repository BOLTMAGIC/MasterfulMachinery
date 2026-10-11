package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.FlexItems;
import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "mm:input/flex_match": for each kind of item an "mm:input/flex" entry ("from") takes, also needs the item its
 * template names, e.g. the matching crux for each seed ({"item": "{ma:crux}", "chance": 0}). A kind whose matching
 * item isn't in the input ports (or doesn't exist) isn't taken. "chance" works like on mm:input/consume.
 */
public class FlexMatchIngredientEntry implements IRecipeIngredientEntry {

    @Getter
    private final String from;
    private final FlexTemplate template;
    @Getter
    private final int count;
    private final double chance;
    private final Map<Item, ItemStack> required = new ConcurrentHashMap<>();

    public FlexMatchIngredientEntry(String from, FlexTemplate template, int count, double chance) {
        this.from = from;
        this.template = template;
        this.count = count;
        this.chance = chance;
    }

    /** @return the item needed next to this kind (count 1), or empty if there is none (the kind isn't accepted) */
    public ItemStack requiredFor(ItemStack kind) {
        if (kind.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return required.computeIfAbsent(kind.getItem(), item -> FlexItems.resolve(template, kind));
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        // the flex input only takes kinds whose matching items are in the ports
        return true;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        for (ItemStack kind : state.getCaptured(from)) {
            if (chance <= 0 || !ChanceUtils.shouldProceed(chance)) {
                continue;
            }
            ItemStack needed = requiredFor(kind);
            if (needed.isEmpty()) {
                continue;
            }
            int remaining = count;
            for (ItemPortStorage storage : itemStorages) {
                remaining = storage.extract(s -> ItemStack.isSameItem(s, needed), remaining);
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        FlexRecipeIngredientEntry input = null;
        for (var entry : model.inputs().inputs()) {
            if (entry instanceof FlexRecipeIngredientEntry flex && flex.getName().equals(from)) {
                input = flex;
            }
        }
        if (input == null) {
            return;
        }
        var stacks = new ArrayList<ItemStack>();
        for (ItemStack kind : input.acceptedExamples()) {
            stacks.add(requiredFor(kind).copyWithCount(count));
        }
        if (stacks.isEmpty()) {
            return;
        }
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.INPUT, slot.getInnerX(), slot.getInnerY());
        slot.setBadgeCount(count);
        if (chance <= 0) {
            slot.setBadgeNotUsed();
        }
        if (count > 1 || chance <= 0) {
            rSlot.setOverlay(new SlotBadgeDrawable(count, chance <= 0), 0, 0);
        }
        rSlot.addItemStacks(stacks);
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.flex_match").withStyle(ChatFormatting.DARK_AQUA));
            if (chance <= 0) {
                list.add(Component.translatable("jei.mm.recipe.not_consumed").withStyle(ChatFormatting.DARK_AQUA));
            } else if (chance < 1) {
                list.add(Component.literal(String.format("%.2f", chance * 100) + "% Chance of Consumption").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
        input.addToJeiGroup(rSlot);
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("from", from);
        json.addProperty("count", count);
        json.addProperty("chance", chance);
        var needed = new com.google.gson.JsonArray();
        for (ItemStack kind : state.getCaptured(from)) {
            needed.add(String.valueOf(requiredFor(kind)));
        }
        json.add("required", needed);
        return json;
    }
}
