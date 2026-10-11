package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.OutputSimulation;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntry;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * "mm:input/flex": takes, in one run, every kind of item in the input ports matching its ingredient (usually a
 * tag), "count" of each, and remembers the kinds under its "as" name so "mm:output/flex" entries make one output
 * per kind. Mixed seeds go in together and come out together. Items whose output doesn't exist stay in the port,
 * and kinds whose outputs wouldn't fit next to the others wait for a later run. "max_types" caps the kinds per run.
 */
public class FlexRecipeIngredientEntry implements IRecipeIngredientEntry {

    @Getter
    private final String name;
    @Getter
    private final BaseItemPortIngredient ingredient;
    private final int maxTypes;
    private final List<FlexRecipeOutputEntry> outputs = new ArrayList<>();

    private volatile List<ItemStack> accepted;
    /** The JEI slot made by the last setRecipe call, for flex outputs to link to. */
    @Getter
    private IRecipeSlotBuilder lastJeiSlot;

    public FlexRecipeIngredientEntry(String name, BaseItemPortIngredient ingredient, int maxTypes) {
        this.name = name;
        this.ingredient = ingredient;
        this.maxTypes = maxTypes;
    }

    public void bindOutput(FlexRecipeOutputEntry output) {
        outputs.add(output);
    }

    /** Whether the item matches this entry and every flex output made from it exists. */
    public boolean accepts(@Nullable Level level, ItemStack stack) {
        if (!ingredient.matches(stack)) {
            return false;
        }
        for (FlexRecipeOutputEntry output : outputs) {
            if (output.resolve(level, stack).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the kinds of item to take this run (count 1 each), in port slot order: accepted, with enough of each
     * in the ports, and with outputs that fit together in the output ports
     */
    private List<ItemStack> pick(Level level, @Nullable RecipeStorages storages, RecipeStateModel state) {
        var picked = new ArrayList<ItemStack>();
        if (storages == null) {
            return picked;
        }
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        var tried = new ArrayList<ItemStack>();
        OutputSimulation simulation = null;
        for (ItemPortStorage storage : itemStorages) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots() && picked.size() < maxTypes; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!ingredient.matches(stack) || tried.stream().anyMatch(t -> ItemStack.isSameItemSameTags(t, stack))) {
                    continue;
                }
                tried.add(stack);
                if (!accepts(level, stack) || !enoughOf(itemStorages, stack)) {
                    continue;
                }
                var kindOutputs = new ArrayList<SingleItemPortIngredient>();
                for (FlexRecipeOutputEntry output : outputs) {
                    SingleItemPortIngredient made = output.outputFor(level, stack, state);
                    if (made != null) {
                        kindOutputs.add(made);
                    }
                }
                if (simulation == null) {
                    simulation = new OutputSimulation(storages);
                }
                // a kind whose output doesn't fit next to the others waits for a later run
                if (simulation.tryAdd(level, kindOutputs)) {
                    picked.add(stack.copyWithCount(1));
                }
            }
        }
        return picked;
    }

    private boolean enoughOf(List<ItemPortStorage> itemStorages, ItemStack stack) {
        int remaining = ingredient.getCount();
        Predicate<ItemStack> same = exactly(stack);
        for (ItemPortStorage s : itemStorages) {
            remaining = s.canExtract(same, remaining);
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static Predicate<ItemStack> exactly(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        return s -> ItemStack.isSameItemSameTags(s, copy);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = pick(level, storages, state);
        state.setCaptured(name, picked);
        return !picked.isEmpty();
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = state.getCaptured(name);
        if (picked.isEmpty()) {
            picked = pick(level, storages, state);
            state.setCaptured(name, picked);
        }
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        for (ItemStack kind : picked) {
            int remaining = ingredient.getCount();
            Predicate<ItemStack> same = exactly(kind);
            for (ItemPortStorage storage : itemStorages) {
                remaining = storage.extract(same, remaining);
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    /** The items this entry accepts with no world around (JEI): matching items whose outputs all exist. */
    public List<ItemStack> acceptedExamples() {
        var cached = accepted;
        if (cached != null) {
            return cached;
        }
        var list = new ArrayList<ItemStack>();
        for (ItemStack example : ingredient.exampleStacks()) {
            if (accepts(null, example)) {
                list.add(example);
            }
        }
        // tags may not be loaded yet; only keep a result once there is one
        if (!list.isEmpty()) {
            accepted = List.copyOf(list);
        }
        return list;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.INPUT, slot.getInnerX(), slot.getInnerY());
        int count = ingredient.getCount();
        slot.setBadgeCount(count);
        if (count > 1) {
            rSlot.setOverlay(new SlotBadgeDrawable(count, false), 0, 0);
        }
        rSlot.addItemStacks(acceptedExamples());
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        rSlot.addRichTooltipCallback((v, list) -> list.add(Component.translatable("jei.mm.recipe.flex_input").withStyle(ChatFormatting.DARK_AQUA)));
        lastJeiSlot = rSlot;
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("as", name);
        json.addProperty("maxTypes", maxTypes);
        json.addProperty("captured", String.valueOf(state.getCaptured(name)));
        json.add("ingredient", ingredient.debugInput(level, storages, new JsonObject()));
        return json;
    }
}
