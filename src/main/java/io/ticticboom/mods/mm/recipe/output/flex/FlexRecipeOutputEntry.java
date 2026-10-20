package io.ticticboom.mods.mm.recipe.output.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.FlexItems;
import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.flex.OutputSimulation;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "mm:output/flex": for each kind of item a "mm:input/flex" entry ("from") took, outputs the item worked out from it, using the
 * "match"/"item" id template, or what a script picks in MMEvents.flexOutput.
 */
public class FlexRecipeOutputEntry implements IRecipeOutputEntry {

    /** An item kind (item + NBT) used as a cache key. */
    private record Key(Item item, @Nullable CompoundTag tag) {
        static Key of(ItemStack stack) {
            return new Key(stack.getItem(), stack.getTag() == null ? null : stack.getTag().copy());
        }
    }

    private record OutputKey(Key item, int amount) {
    }

    @Getter
    private final String from;
    private final FlexTemplate template;
    private final int count;
    private final boolean copyNbt;
    private final double chance;
    private final boolean perTick;
    @Getter
    @Nullable
    private final String scaleBy;

    private ResourceLocation recipeId;
    private FlexRecipeIngredientEntry input;

    // the client (JEI) and the integrated server share recipe objects; only the server asks scripts
    private final Map<Key, ItemStack> templateResults = new ConcurrentHashMap<>();
    private final Map<Key, ItemStack> serverResults = new ConcurrentHashMap<>();
    private final Map<OutputKey, SingleItemPortIngredient> outputIngredients = new ConcurrentHashMap<>();
    private volatile List<DisplayedOutput> displayed;

    private boolean shouldRun = true;

    public FlexRecipeOutputEntry(String from, FlexTemplate template, int count, boolean copyNbt, double chance, boolean perTick, @Nullable String scaleBy) {
        this.from = from;
        this.template = template;
        this.count = count;
        this.copyNbt = copyNbt;
        this.chance = chance;
        this.perTick = perTick;
        this.scaleBy = scaleBy;
    }

    public void bind(ResourceLocation recipeId, FlexRecipeIngredientEntry input) {
        this.recipeId = recipeId;
        this.input = input;
    }

    /**
     * @param level null (or client side) for the template result only, with no scripts asked
     * @return the output item made from this input (count 1), or empty if this input isn't accepted
     */
    public ItemStack resolve(@Nullable Level level, ItemStack inputStack) {
        if (inputStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Key key = Key.of(inputStack);
        ItemStack templated = templateResults.computeIfAbsent(key, k -> fromTemplate(inputStack));
        if (level == null || level.isClientSide()) {
            return templated;
        }
        return serverResults.computeIfAbsent(key, k -> MMInteropManager.KUBEJS
                .map(kjs -> kjs.resolveFlexOutput(recipeId, from, inputStack.copyWithCount(1), templated.copy()))
                .map(picked -> picked.isEmpty() ? ItemStack.EMPTY : picked.copyWithCount(1))
                .orElse(templated));
    }

    private ItemStack fromTemplate(ItemStack inputStack) {
        ItemStack result = FlexItems.resolve(template, inputStack);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (copyNbt && inputStack.getTag() != null) {
            result.setTag(inputStack.getTag().copy());
        }
        return result;
    }

    private int amount(RecipeStateModel state) {
        return scaleBy == null ? count : IPortIngredient.scaleAmount(count, state.getCount(scaleBy));
    }

    /**
     * @param captured one kind of item the flex input took
     * @return what to put in the output ports for it, or null if there is nothing to output
     */
    @Nullable
    public SingleItemPortIngredient outputFor(Level level, ItemStack captured, RecipeStateModel state) {
        ItemStack result = resolve(level, captured);
        int amount = amount(state);
        if (result.isEmpty() || amount <= 0) {
            return null;
        }
        return outputIngredients.computeIfAbsent(new OutputKey(Key.of(result), amount), k ->
                new SingleItemPortIngredient(ForgeRegistries.ITEMS.getKey(result.getItem()), amount, result.getTag(), true));
    }

    /** What to put in the output ports this run: one output per kind of item the flex input took. */
    private List<SingleItemPortIngredient> outputsFor(Level level, RecipeStateModel state) {
        var outputs = new ArrayList<SingleItemPortIngredient>();
        for (ItemStack captured : state.getCaptured(from)) {
            SingleItemPortIngredient output = outputFor(level, captured, state);
            if (output == null) {
                // e.g. a reload removed the output item while the recipe was running: skip it, don't block the machine
                Ref.LOG.debug("Flex recipe {} has no output for {}", recipeId, captured);
                continue;
            }
            outputs.add(output);
        }
        return outputs;
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        shouldRun = ChanceUtils.shouldProceed(chance);
        if (!shouldRun) {
            return true;
        }
        var outputs = outputsFor(level, state);
        if (outputs.size() == 1) {
            return outputs.get(0).canOutput(level, storages, state);
        }
        // several kinds share the same empty slots: they have to fit together
        return outputs.isEmpty() || new OutputSimulation(storages).tryAdd(level, outputs);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun) {
            for (SingleItemPortIngredient output : outputsFor(level, state)) {
                output.output(level, storages, state);
            }
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (perTick && shouldRun) {
            for (SingleItemPortIngredient output : outputsFor(level, state)) {
                output.output(level, storages, state);
            }
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    /** The output for each of these input items, in the same order (JEI and the controller screen). */
    private List<ItemStack> exampleOutputs(List<ItemStack> inputs) {
        var outputs = new ArrayList<ItemStack>();
        for (ItemStack example : inputs) {
            outputs.add(resolve(null, example).copyWithCount(count));
        }
        return outputs;
    }

    @Override
    public List<DisplayedOutput> displayedOutputs() {
        var cached = displayed;
        if (cached != null) {
            return cached;
        }
        var outputs = input == null ? List.<ItemStack>of() : exampleOutputs(input.acceptedExamples());
        if (outputs.isEmpty()) {
            return List.of();
        }
        // One entry: JEI shows a flex output as a single cycling slot and sizes the recipe (and, through MMJeiPlugin,
        // every MM category) by this count, so one per kind would reserve a row per kind. The controller screen lists
        // the kinds a run actually took through displayedOutputs(state).
        var result = new ArrayList<DisplayedOutput>();
        for (ItemStack output : outputs) {
            // the first kind with a valid output stands for the rest
            if (!output.isEmpty()) {
                var ingredient = new SingleItemPortIngredient(ForgeRegistries.ITEMS.getKey(output.getItem()), count, output.getTag(), true);
                result.add(new DisplayedOutput(ingredient, chance));
                break;
            }
        }
        displayed = List.copyOf(result);
        return displayed;
    }

    @Override
    public List<DisplayedOutput> displayedOutputs(@Nullable RecipeStateModel state) {
        // If we have a state with captured items, show outputs for those - all of them
        if (state != null && input != null) {
            var captured = state.getCaptured(from);
            if (!captured.isEmpty()) {
                var result = new ArrayList<DisplayedOutput>();
                for (ItemStack item : captured) {
                    ItemStack output = resolve(null, item);
                    if (!output.isEmpty()) {
                        var ingredient = new SingleItemPortIngredient(ForgeRegistries.ITEMS.getKey(output.getItem()), count, output.getTag(), true);
                        result.add(new DisplayedOutput(ingredient, chance));
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        // Fallback to default behavior if no state or no captured items
        return displayedOutputs();
    }

    @Override
    public int jeiSlotCount(RecipeModel model) {
        if (input == null || exampleOutputs(input.examplesFor(model)).isEmpty()) {
            return 0;
        }
        return input.jeiLanes(model);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        var outputs = input == null ? List.<ItemStack>of() : exampleOutputs(input.examplesFor(model));
        if (outputs.isEmpty()) {
            return;
        }
        // one slot per lane of the flex input, listing the outputs in the same (rotated) order
        int lanes = input.jeiLanesWithFocus(model, focus);
        for (int lane = 0; lane < lanes; lane++) {
            addJeiSlot(builder, helpers, grid, FlexRecipeIngredientEntry.forLane(outputs, lane), lane);
        }
    }

    private void addJeiSlot(IRecipeLayoutBuilder builder, IJeiHelpers helpers, SlotGrid grid, List<ItemStack> outputs, int lane) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, slot.getInnerX(), slot.getInnerY());
        slot.setBadgeCount(count);
        if (count > 1) {
            rSlot.setOverlay(new SlotBadgeDrawable(count, false), 0, 0);
        }
        rSlot.addItemStacks(outputs);
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        // listed in the same order as the input's slot of this lane: linked by MMRecipeCategory so they turn together
        input.addToJeiLane(lane, rSlot);
        double percent = chance * 100.0;
        String percentStr = new java.math.BigDecimal(Double.toString(percent)).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.flex_output").withStyle(ChatFormatting.DARK_AQUA));
            if (chance < 1) {
                list.add(Component.literal(percentStr + "% Chance of Output").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.translatable("jei.mm.recipe.output_per_tick").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (scaleBy != null) {
                list.add(Component.translatable("jei.mm.recipe.scaled_by", scaleBy).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("from", from);
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        json.addProperty("amount", amount(model));
        var resolved = new com.google.gson.JsonArray();
        for (ItemStack captured : model.getCaptured(from)) {
            resolved.add(String.valueOf(resolve(level, captured)));
        }
        json.add("resolved", resolved);
        return json;
    }
}
