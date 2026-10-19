package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.mysticalagriculture.MACrops;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import io.ticticboom.mods.mm.recipe.flex.OutputSimulation;
import io.ticticboom.mods.mm.util.ChanceUtils;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * "mm:input/flex": takes, in one run, every kind of item in the input ports matching its ingredient (usually a
 * tag), "count" of each, and remembers the kinds under its "as" name so "mm:output/flex" entries make one output
 * per kind. Mixed seeds go in together and come out together. Items whose output doesn't exist stay in the port,
 * and kinds whose outputs wouldn't fit next to the others wait for a later run. "max_types" caps the kinds per run.
 * "chance" (default 1) is the chance each kind is consumed; 0 keeps the items in the port (like a catalyst).
 * The number of kinds is stored under the "as" name (usable by "scale_by") and sets the run's cost multiplier for
 * the recipe's other consumed inputs (see FlexCost).
 * "exclude" lists items that are never taken: ids, "#tags" and "modid:*". With Mystical Agriculture loaded, seeds
 * of disabled crops are never taken, and "min_tier" / "max_tier" keep only MA seeds of those crop tiers.
 * "mm:input/flex_match" entries add a matching item each kind needs (e.g. its crux).
 */
public class FlexRecipeIngredientEntry implements IRecipeIngredientEntry {

    @Getter
    private final String name;
    @Getter
    private final BaseItemPortIngredient ingredient;
    @Getter
    private final int maxTypes;
    private final double chance;
    private final List<Predicate<ItemStack>> exclude;
    @Nullable
    private final Integer minTier;
    @Nullable
    private final Integer maxTier;
    /** The recipe's "flex_cost", or null for the config default. */
    @Nullable
    private FlexCost cost;
    private final List<FlexRecipeOutputEntry> outputs = new ArrayList<>();
    private final List<FlexMatchIngredientEntry> matches = new ArrayList<>();

    private volatile List<ItemStack> accepted;
    /** The JEI slots of the last setRecipe call that list one item per kind, in the same order (linked together).
     * -- GETTER --
     * The JEI slots to link, see
     * .
     */
    @Getter
    private final List<IRecipeSlotBuilder> jeiGroup = new ArrayList<>();

    public FlexRecipeIngredientEntry(String name, BaseItemPortIngredient ingredient, int maxTypes, double chance,
                                     List<Predicate<ItemStack>> exclude, @Nullable Integer minTier, @Nullable Integer maxTier) {
        this.name = name;
        this.ingredient = ingredient;
        this.maxTypes = maxTypes;
        this.chance = chance;
        this.exclude = exclude;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    public void bindMatch(FlexMatchIngredientEntry match) {
        matches.add(match);
    }

    /** Adds a JEI slot that lists one item per accepted kind, so it turns together with this entry's slot. */
    public void addToJeiGroup(IRecipeSlotBuilder slot) {
        jeiGroup.add(slot);
    }

    public void bindCost(@Nullable FlexCost cost) {
        this.cost = cost;
    }

    /** Stores the kinds this run takes, their number, and the cost multiplier that follows from it. */
    private void setPicked(RecipeStateModel state, List<ItemStack> picked) {
        state.setCaptured(name, picked);
        state.setCount(name, picked.size());
        state.setCostMultiplier((cost == null ? FlexCost.global() : cost).multiplier(picked.size()));
    }

    /**
     * Takes the last kind out of this run, when the recipe can't pay its other inputs for all of them.
     *
     * @return false if there was at most one kind left
     */
    public boolean dropLastKind(RecipeStateModel state) {
        var picked = state.getCaptured(name);
        if (picked.size() <= 1) {
            return false;
        }
        setPicked(state, picked.subList(0, picked.size() - 1));
        return true;
    }

    public void bindOutput(FlexRecipeOutputEntry output) {
        outputs.add(output);
    }

    /**
     * Whether the item matches this entry (and isn't excluded or filtered out), and every flex output and matching
     * item made from it exists.
     */
    public boolean accepts(@Nullable Level level, ItemStack stack) {
        if (!ingredient.matches(stack)) {
            return false;
        }
        for (Predicate<ItemStack> excluded : exclude) {
            if (excluded.test(stack)) {
                return false;
            }
        }
        MACrops.Info crop = MACrops.forSeeds(stack.getItem());
        if (crop != null && !crop.enabled()) {
            return false;
        }
        if (minTier != null || maxTier != null) {
            if (crop == null || (minTier != null && crop.tier() < minTier) || (maxTier != null && crop.tier() > maxTier)) {
                return false;
            }
        }
        for (FlexMatchIngredientEntry match : matches) {
            if (match.requiredFor(stack).isEmpty()) {
                return false;
            }
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
        // matching items (e.g. cruxes) already counted for kinds picked before
        var reserved = new HashMap<Item, Integer>();
        OutputSimulation simulation = null;
        for (ItemPortStorage storage : itemStorages) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots() && picked.size() < maxTypes; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!ingredient.matches(stack) || tried.stream().anyMatch(t -> ItemStack.isSameItemSameTags(t, stack))) {
                    continue;
                }
                tried.add(stack);
                if (!accepts(level, stack) || !enoughOf(itemStorages, stack) || !matchesAvailable(itemStorages, stack, reserved)) {
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
                    for (FlexMatchIngredientEntry match : matches) {
                        reserved.merge(match.requiredFor(stack).getItem(), match.getCount(), Integer::sum);
                    }
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

    /** Whether the matching items this kind needs are in the ports, next to those reserved for other kinds. */
    private boolean matchesAvailable(List<ItemPortStorage> itemStorages, ItemStack kind, Map<Item, Integer> reserved) {
        var needed = new HashMap<Item, Integer>();
        for (FlexMatchIngredientEntry match : matches) {
            needed.merge(match.requiredFor(kind).getItem(), match.getCount(), Integer::sum);
        }
        for (var entry : needed.entrySet()) {
            Item item = entry.getKey();
            int remaining = entry.getValue() + reserved.getOrDefault(item, 0);
            for (ItemPortStorage s : itemStorages) {
                remaining = s.canExtract(st -> st.is(item), remaining);
                if (remaining <= 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static Predicate<ItemStack> exactly(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        return s -> ItemStack.isSameItemSameTags(s, copy);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = pick(level, storages, state);
        setPicked(state, picked);
        return !picked.isEmpty();
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = state.getCaptured(name);
        if (picked.isEmpty()) {
            picked = pick(level, storages, state);
            setPicked(state, picked);
        }
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        for (ItemStack kind : picked) {
            if (chance <= 0 || !ChanceUtils.shouldProceed(chance)) {
                continue;
            }
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

    /** JEI shows at most this many ingredients in one slot; longer lists are split into pages. */
    public static final int JEI_SLOT_LIMIT = 100;
    /** Config keys of a JEI page copy of a recipe (see {@link #jeiPages()} and FlexJeiPages). */
    public static final String JEI_PAGE = "_jeiPage";
    public static final String JEI_PAGE_TIER = "_jeiPageTier";
    public static final String JEI_PAGE_PART = "_jeiPagePart";

    /**
     * One JEI page of this entry's accepted items.
     *
     * @param tier Mystical Agriculture crop tier of the items, or -1
     * @param part "2/3" when a tier (or the whole list) needed several pages, else ""
     */
    public record JeiPage(int tier, String part, List<ItemStack> items) {
    }

    /**
     * The accepted items split for JEI: with Mystical Agriculture one page per crop tier (lowest first), and any
     * group longer than {@link #JEI_SLOT_LIMIT} in parts of that size. Items keep their tag order.
     */
    public List<JeiPage> jeiPages() {
        var groups = new TreeMap<Integer, List<ItemStack>>();
        for (ItemStack item : acceptedExamples()) {
            MACrops.Info crop = MACrops.forSeeds(item.getItem());
            groups.computeIfAbsent(crop == null ? Integer.MAX_VALUE : crop.tier(), k -> new ArrayList<>()).add(item);
        }
        var pages = new ArrayList<JeiPage>();
        for (var group : groups.entrySet()) {
            List<ItemStack> items = group.getValue();
            int tier = group.getKey() == Integer.MAX_VALUE ? -1 : group.getKey();
            int parts = (items.size() + JEI_SLOT_LIMIT - 1) / JEI_SLOT_LIMIT;
            for (int i = 0; i < parts; i++) {
                var chunk = items.subList(i * JEI_SLOT_LIMIT, Math.min(items.size(), (i + 1) * JEI_SLOT_LIMIT));
                pages.add(new JeiPage(tier, parts > 1 ? (i + 1) + "/" + parts : "", List.copyOf(chunk)));
            }
        }
        return pages;
    }

    /** The items to list in JEI for this recipe: its page's items for a JEI page copy, else all accepted items. */
    public List<ItemStack> examplesFor(RecipeModel model) {
        if (model.config() == null || !model.config().has(JEI_PAGE)) {
            return acceptedExamples();
        }
        int page = model.config().get(JEI_PAGE).getAsInt();
        var pages = jeiPages();
        return page >= 0 && page < pages.size() ? pages.get(page).items() : List.of();
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
        var examples = examplesFor(model);
        if (maxTypes > 1 && !examples.isEmpty()) {
            var firstItem = examples.get(0);
            var repeated = new ArrayList<ItemStack>();
            for (int i = 0; i < maxTypes; i++) {
                repeated.add(firstItem.copy());
            }
            rSlot.addItemStacks(repeated);
        } else {
            rSlot.addItemStacks(examples);
        }
        jeiGroup.clear();
        jeiGroup.add(rSlot);
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        if (chance <= 0) {
            slot.setBadgeNotUsed();
            rSlot.setOverlay(new SlotBadgeDrawable(count, true), 0, 0);
        }
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.flex_input").withStyle(ChatFormatting.DARK_AQUA));
            if (chance <= 0) {
                list.add(Component.translatable("jei.mm.recipe.not_consumed").withStyle(ChatFormatting.DARK_AQUA));
            } else if (chance < 1) {
                list.add(Component.literal(String.format("%.2f", chance * 100) + "% Chance of Consumption").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("as", name);
        json.addProperty("maxTypes", maxTypes);
        json.addProperty("chance", chance);
        if (minTier != null) json.addProperty("minTier", minTier);
        if (maxTier != null) json.addProperty("maxTier", maxTier);
        json.addProperty("captured", String.valueOf(state.getCaptured(name)));
        json.add("ingredient", ingredient.debugInput(level, storages, new JsonObject()));
        return json;
    }

    /** Get the display content for this flex input, considering the current recipe state. */
    public List<PortContent> getDisplayContent(@Nullable RecipeStateModel state) {
        if (state == null) {
            // No state, return generic display
            var content = ingredient.display();
            return content != null ? List.of(content) : List.of();
        }
        // Return display content for each captured item in the state
        var captured = state.getCaptured(name);
        var result = new ArrayList<PortContent>();
        for (ItemStack item : captured) {
            PortContent content = new SingleItemPortIngredient(
                ForgeRegistries.ITEMS.getKey(item.getItem()),
                ingredient.getCount(),
                item.getTag(),
                true
            ).display();
            if (content != null) {
                result.add(content);
            }
        }
        // Fallback to generic display if no captured items
        if (result.isEmpty()) {
            var content = ingredient.display();
            if (content != null) result.add(content);
        }
        return result;
    }
}
