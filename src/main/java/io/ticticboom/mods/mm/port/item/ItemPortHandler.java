package io.ticticboom.mods.mm.port.item;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Map;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemPortHandler extends ItemStackHandler {

    // OPTIONAL_CODEC, not CODEC: 1.20.5 split these, and the strict one rejects empty stacks
    // ("Value must be within range [1;99]", "Item must not be minecraft:air"). Port slots are
    // empty most of the time, and every slot is serialised on each block update.
    public static Codec<List<ItemStack>> STACKS_CODEC = Codec.list(ItemStack.OPTIONAL_CODEC);
    private final INotifyChangeFunction changed;
    private final int slotCapacity; // 0 = use item default
    public static final int MAX_SLOT_CAPACITY = 16384;
    private static final int HARD_MAX = MAX_SLOT_CAPACITY;
    private static final int CODEC_MAX_COUNT = 99;

    private final int[] actualCounts;
    private final BitSet emptySlots = new BitSet();
    private final Map<Item, BitSet> partialSlots = new HashMap<>();
    private final Map<Item, BitSet> fullSlots = new HashMap<>();
    private final Map<Item, Long> itemTotals = new HashMap<>();
    private final Item[] indexedItems;
    private final int[] indexedCounts;
    private long contentRevision;

    public ItemPortHandler(int size, int slotCapacity, INotifyChangeFunction changed) {
        super(size);
        this.changed = changed;
        // normalize slotCapacity
        if (slotCapacity <= 0) {
            this.slotCapacity = 0;
        } else {
            this.slotCapacity = Math.min(HARD_MAX, slotCapacity);
        }
        this.actualCounts = new int[size];
        for (int i = 0; i < size; i++) this.actualCounts[i] = 0;
        this.indexedItems = new Item[size];
        this.indexedCounts = new int[size];
        this.emptySlots.set(0, size);
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    public int getConfiguredSlotCapacity() {
        return slotCapacity;
    }

    public Tag serializeStacks(@Nullable HolderLookup.Provider registries) {
        // store both display stacks and actualCounts into a compound
        var compound = new CompoundTag();
        var encodable = new ArrayList<ItemStack>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.getCount() > CODEC_MAX_COUNT) {
                ItemStack clamped = stack.copy();
                clamped.setCount(CODEC_MAX_COUNT);
                encodable.add(clamped);
            } else {
                encodable.add(stack);
            }
        }
        var tag = ops(registries).withEncoder(STACKS_CODEC).apply(encodable);
        compound.put("stacks", tag.getOrThrow(__msg -> { Ref.LOG.error(__msg); return new IllegalStateException(__msg); }));
        compound.putIntArray("counts", actualCounts);
        return compound;
    }

    public void deserializeStacks(Tag nbt, @Nullable HolderLookup.Provider registries) {
        if (!(nbt instanceof CompoundTag ct)) return;
        // read stacks
        Tag stacksTag = ct.get("stacks");
        if (stacksTag != null) {
            var res = ops(registries).withDecoder(STACKS_CODEC).apply(stacksTag);
            var pair = res.getOrThrow(__msg -> { Ref.LOG.error(__msg); return new IllegalStateException(__msg); });
            this.stacks.clear();
            List<ItemStack> list = pair.getFirst();
            for (int i = 0; i < Math.min(list.size(), stacks.size()); i++) {
                stacks.set(i, list.get(i));
            }
            if (list.size() > stacks.size()) {
                Ref.LOG.warn("A port lost {} slots after its config was made smaller; items in them were removed", list.size() - stacks.size());
            }
        }
        // read counts
        if (ct.contains("counts")) {
            int[] arr = ct.getIntArray("counts");
            int len = Math.min(arr.length, actualCounts.length);
            System.arraycopy(arr, 0, actualCounts, 0, len);
        } else {
            for (int i = 0; i < stacks.size() && i < actualCounts.length; i++) {
                actualCounts[i] = stacks.get(i).getCount();
            }
        }
        restoreDisplayCounts();
        for (int i = 0; i < stacks.size(); i++) {
            reindexSlot(i);
        }
        contentRevision++;
    }

    /**
     * Copies the stacks and actual counts of a handler of the same size, without going through the codecs
     * (used for simulation copies, which are made several times per recipe scan).
     */
    public void copyContentsFrom(ItemPortHandler other) {
        int size = Math.min(stacks.size(), other.stacks.size());
        for (int i = 0; i < size; i++) {
            stacks.set(i, other.stacks.get(i).copy());
            actualCounts[i] = other.actualCounts[i];
            reindexSlot(i);
        }
        contentRevision++;
    }

    private static DynamicOps<Tag> ops(@Nullable HolderLookup.Provider registries) {
        return registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE);
    }

    private void restoreDisplayCounts() {
        for (int i = 0; i < stacks.size() && i < actualCounts.length; i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                actualCounts[i] = 0;
                continue;
            }
            if (actualCounts[i] <= 0) {
                actualCounts[i] = stack.getCount();
            }
            int display = Math.min(HARD_MAX, actualCounts[i]);
            if (stack.getCount() != display) {
                stack.setCount(display);
            }
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        reindexSlot(slot);
        contentRevision++;
        changed.call();
    }

    private void reindexSlot(int slot) {
        Item previous = indexedItems[slot];
        if (previous != null) {
            long previousTotal = itemTotals.getOrDefault(previous, 0L) - indexedCounts[slot];
            if (previousTotal > 0) itemTotals.put(previous, previousTotal);
            else itemTotals.remove(previous);
            indexedCounts[slot] = 0;
            BitSet slots = partialSlots.get(previous);
            if (slots != null) {
                slots.clear(slot);
                if (slots.isEmpty()) partialSlots.remove(previous);
            }
            slots = fullSlots.get(previous);
            if (slots != null) {
                slots.clear(slot);
                if (slots.isEmpty()) fullSlots.remove(previous);
            }
            indexedItems[slot] = null;
        }
        ItemStack stack = getStackInSlot(slot);
        if (actualCounts[slot] <= 0 || stack.isEmpty()) {
            emptySlots.set(slot);
            return;
        }
        emptySlots.clear(slot);
        indexedItems[slot] = stack.getItem();
        indexedCounts[slot] = actualCounts[slot];
        itemTotals.merge(stack.getItem(), (long) actualCounts[slot], Long::sum);
        if (stack.isStackable() && actualCounts[slot] < getSlotLimit(slot)) {
            partialSlots.computeIfAbsent(stack.getItem(), ignored -> new BitSet()).set(slot);
        } else {
            fullSlots.computeIfAbsent(stack.getItem(), ignored -> new BitSet()).set(slot);
        }
    }

    public long contentRevision() {
        return contentRevision;
    }

    public boolean hasEmptySlots() {
        return !emptySlots.isEmpty();
    }

    public boolean hasItem(Item item) {
        return partialSlots.containsKey(item) || fullSlots.containsKey(item);
    }

    public boolean hasCompatiblePartialSlot(ItemStack stack) {
        BitSet slots = partialSlots.get(stack.getItem());
        if (slots == null) return false;
        for (int slot = slots.nextSetBit(0); slot >= 0; slot = slots.nextSetBit(slot + 1)) {
            if (ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack)) return true;
        }
        return false;
    }

    public Set<Item> indexedItemTypes() {
        var result = new HashSet<>(partialSlots.keySet());
        result.addAll(fullSlots.keySet());
        return result;
    }

    public Map<Item, Long> itemTotals() {
        return Map.copyOf(itemTotals);
    }

    public int extractMatching(Item item, Predicate<ItemStack> filter, int count, boolean simulate) {
        int remaining = extractMatching(partialSlots.get(item), filter, count, simulate);
        if (remaining > 0) remaining = extractMatching(fullSlots.get(item), filter, remaining, simulate);
        return remaining;
    }

    private int extractMatching(BitSet slots, Predicate<ItemStack> filter, int remaining, boolean simulate) {
        if (slots == null) return remaining;
        for (int slot = slots.nextSetBit(0); slot >= 0 && remaining > 0; slot = slots.nextSetBit(slot + 1)) {
            ItemStack stack = getStackInSlot(slot);
            if (!filter.test(stack)) continue;
            remaining -= extractItem(slot, remaining, simulate).getCount();
        }
        return remaining;
    }

    public ItemStack insertStackFast(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack;
        BitSet partial = partialSlots.get(stack.getItem());
        if (partial != null) {
            BitSet snapshot = (BitSet) partial.clone();
            for (int slot = snapshot.nextSetBit(0); slot >= 0 && !remaining.isEmpty(); slot = snapshot.nextSetBit(slot + 1)) {
                remaining = insertItem(slot, remaining, simulate);
            }
        }
        BitSet empty = (BitSet) emptySlots.clone();
        for (int slot = empty.nextSetBit(0); slot >= 0 && !remaining.isEmpty(); slot = empty.nextSetBit(slot + 1)) {
            remaining = insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    public long insertionCapacity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        long capacity = 0;
        BitSet partial = partialSlots.get(stack.getItem());
        if (partial != null) {
            for (int slot = partial.nextSetBit(0); slot >= 0; slot = partial.nextSetBit(slot + 1)) {
                if (ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack)) {
                    capacity += getSlotLimit(slot) - actualCounts[slot];
                }
            }
        }
        for (int slot = emptySlots.nextSetBit(0); slot >= 0; slot = emptySlots.nextSetBit(slot + 1)) {
            capacity += !stack.isStackable() ? 1 : slotCapacity > 0 ? slotCapacity : stack.getMaxStackSize();
        }
        return capacity;
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStack stack = getStackInSlot(slot);
        // if no override configured, use item default or 64 fallback
        if (slotCapacity <= 0) {
            if (stack.isEmpty()) {
                return 64;
            }
            // Use ItemStack-sensitive API instead of the deprecated Item#getMaxStackSize()
            return stack.getMaxStackSize();
        }

        // override is set
        if (stack.isEmpty()) {
            // optimistic: allow stackable items up to slotCapacity when empty
            return slotCapacity;
        }

        // slot contains something
        if (!stack.isStackable()) {
            return 1;
        }

        // stackable -> allow override up to slotCapacity
        return slotCapacity;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        // set actual count to whatever the provided stack has (may be clamped)
        actualCounts[slot] = stack.getCount();
        // call super after updating actualCounts so onContentsChanged() sees the new actual state
        super.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (slot < 0 || slot >= getSlots()) {
            return stack;
        }

        ItemStack existing = getStackInSlot(slot);
        Item item = stack.getItem();
        int limit = getSlotLimit(slot);

        // Non-stackable
        if (!stack.isStackable()) {
            if (actualCounts[slot] > 0) {
                return stack; // can't insert
            }
            if (simulate) {
                ItemStack copy = stack.copy();
                copy.setCount(stack.getCount() - 1);
                return copy;
            } else {
                // place one
                // copy once, split off the single placed item and keep the remainder to return
                ItemStack working = stack.copy();
                ItemStack placed = working.split(1); // placed has count 1, working is remainder
                actualCounts[slot] = 1;
                super.setStackInSlot(slot, placed);
                if (working.isEmpty()) return ItemStack.EMPTY;
                return working;
            }
        }

        // If existing same item, try to add
        if (actualCounts[slot] > 0) {
            if (existing.getItem() != item) {
                return stack; // different item
            }
            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }
            int existingCount = actualCounts[slot];
            int space = limit - existingCount;
            if (space <= 0) {
                return stack;
            }
            int toAdd = Math.min(space, stack.getCount());
            if (!simulate) {
                actualCounts[slot] = existingCount + toAdd;
                // update display stack count to min(item max, actual)
                // display should reflect actual stored amount (up to HARD_MAX) so clients see aggregated counts
                int display = Math.min(HARD_MAX, actualCounts[slot]);
                // preserve existing tag when updating count
                ItemStack newDisplay = existing.copy();
                newDisplay.setCount(display);
                super.setStackInSlot(slot, newDisplay);
            }
            ItemStack res = stack.copy();
            res.shrink(toAdd);
            return res;
        }

        // empty slot
        int toPlace = Math.min(limit, stack.getCount());
        if (simulate) {
            ItemStack copy = stack.copy();
            copy.shrink(toPlace);
            return copy;
        } else {
            actualCounts[slot] = toPlace;
            int display = Math.min(HARD_MAX, toPlace);
            // preserve tag when placing
            ItemStack placed = stack.copy();
            placed.setCount(display);
            super.setStackInSlot(slot, placed);
            ItemStack res = stack.copy();
            res.shrink(toPlace);
            return res;
        }
    }

    @SuppressWarnings("unused")
    public int canInsert(Item item, int count) {
        return canInsert(new ItemStack(item), count);
    }

    public int canInsert(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) return count;
        int remainingToInsert = count;
        BitSet partial = partialSlots.get(stack.getItem());
        if (partial != null) for (int slot = partial.nextSetBit(0); slot >= 0 && remainingToInsert > 0; slot = partial.nextSetBit(slot + 1)) {
            if (ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack))
                remainingToInsert -= Math.min(getSlotLimit(slot) - actualCounts[slot], remainingToInsert);
        }
        for (int slot = emptySlots.nextSetBit(0); slot >= 0 && remainingToInsert > 0; slot = emptySlots.nextSetBit(slot + 1)) {
            remainingToInsert -= Math.min(getSlotLimit(slot), remainingToInsert);
        }

        return remainingToInsert;
    }

    // Thread-local toggle to prefer empty slots on insert (used by container quick-move)
    private static final ThreadLocal<Boolean> THREAD_PREFER_EMPTY = ThreadLocal.withInitial(() -> false);

    public static void setThreadPreferEmpty(boolean v) {
        THREAD_PREFER_EMPTY.set(v);
    }

    private static boolean threadPreferEmpty() {
        return THREAD_PREFER_EMPTY.get();
    }

    public int insert(Item item, int count) {
        return insertInternal(new ItemStack(item), count, false);
    }

    /**
     * Insertion method that accepts ItemStack (with NBT tag) and preserves the tag on placed stacks.
     * @param stack The item stack to insert (including NBT data)
     * @param count The number of items to insert
     * @return The number of items that could not be inserted
     */
    public int insert(ItemStack stack, int count) {
        if (stack.isEmpty()) return count;
        return insertInternal(stack, count, true);
    }

    /**
     * Internal helper method that handles the two-pass insertion logic.
     * @param template The item stack template to insert (contains item and optionally NBT)
     * @param count The number of items to insert
     * @param checkNbt Whether to check NBT compatibility when merging stacks
     * @return The number of items that could not be inserted
     */
    private int insertInternal(ItemStack template, int count, boolean checkNbt) {
        int remainingToInsert = count;
        if (threadPreferEmpty()) {
            remainingToInsert = insertIntoEmptySlots(template, remainingToInsert, checkNbt);
            remainingToInsert = mergeIntoExistingStacks(template, remainingToInsert, checkNbt);
        } else {
            remainingToInsert = mergeIntoExistingStacks(template, remainingToInsert, checkNbt);
            remainingToInsert = insertIntoEmptySlots(template, remainingToInsert, checkNbt);
        }
        return remainingToInsert;
    }

    /**
     * Helper method to insert items into empty slots.
     * @param template The item stack template to insert
     * @param remainingToInsert The number of items remaining to insert
     * @param checkNbt When true, copies the template stack preserving NBT; when false, creates a new ItemStack with only the item type
     * @return The number of items that could not be inserted
     */
    private int insertIntoEmptySlots(ItemStack template, int remainingToInsert, boolean checkNbt) {
        for (int slot = emptySlots.nextSetBit(0); slot >= 0 && remainingToInsert > 0; slot = emptySlots.nextSetBit(slot + 1)) {
            int limit = getSlotLimit(slot);
            int maxPerSlot = Math.max(1, limit);
            int toPlace = Math.min(maxPerSlot, remainingToInsert);
            actualCounts[slot] = toPlace;
            ItemStack placed;
            if (checkNbt) {
                placed = template.copy();
                placed.setCount(Math.min(HARD_MAX, toPlace));
            } else {
                placed = new ItemStack(template.getItem(), Math.min(HARD_MAX, toPlace));
            }
            super.setStackInSlot(slot, placed);
            remainingToInsert -= toPlace;
        }
        return remainingToInsert;
    }

    /**
     * Helper method to merge items into existing compatible stacks.
     * @param template The item stack template to insert
     * @param remainingToInsert The number of items remaining to insert
     * @param checkNbt Whether to check NBT compatibility when merging stacks (true) or only match by item type (false)
     * @return The number of items that could not be inserted
     */
    private int mergeIntoExistingStacks(ItemStack template, int remainingToInsert, boolean checkNbt) {
        BitSet partial = partialSlots.get(template.getItem());
        if (partial == null) return remainingToInsert;
        for (int slot = partial.nextSetBit(0); slot >= 0 && remainingToInsert > 0; slot = partial.nextSetBit(slot + 1)) {
            ItemStack existing = getStackInSlot(slot);
            if (checkNbt && !ItemStack.isSameItemSameComponents(existing, template)) continue;
            int limit = getSlotLimit(slot);
            int space = limit - actualCounts[slot];
            if (space <= 0) continue;
            int toMove = Math.min(space, remainingToInsert);
            actualCounts[slot] = actualCounts[slot] + toMove;
            int display = Math.min(HARD_MAX, actualCounts[slot]);
            ItemStack newDisplay = existing.copy();
            newDisplay.setCount(display);
            super.setStackInSlot(slot, newDisplay);
            remainingToInsert -= toMove;
        }
        return remainingToInsert;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;
        if (actualCounts[slot] <= 0) return ItemStack.EMPTY;
        ItemStack display = getStackInSlot(slot);
        int toExtract = Math.min(amount, actualCounts[slot]);
        if (simulate) {
            ItemStack s = display.copy();
            s.setCount(toExtract);
            return s;
        }
        // perform removal
        actualCounts[slot] -= toExtract;
        if (actualCounts[slot] <= 0) {
            super.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            int displayCount = Math.min(HARD_MAX, actualCounts[slot]);
            ItemStack newDisplay = display.copy();
            newDisplay.setCount(displayCount);
            super.setStackInSlot(slot, newDisplay);
        }
        ItemStack res = display.copy();
        res.setCount(toExtract);
        return res;
    }

    /**
     * Returns the actual number of items stored in the logical slot (may exceed the displayed stack size).
     */
    public int getActualCount(int slot) {
        if (slot < 0 || slot >= actualCounts.length) return 0;
        return actualCounts[slot];
    }

    /**
     * Returns an ItemStack suitable for display/use in the GUI where the count reflects the actual stored amount.
     * If the slot is empty, returns ItemStack.EMPTY.
     */
    public ItemStack getActualDisplayStack(int slot) {
        ItemStack disp = getStackInSlot(slot);
        if (disp.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = disp.copy();
        copy.setCount(actualCounts[slot]);
        return copy;
    }

    /**
     * Clears all stacks and actual counts.
     */
    public void clearAll() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
            actualCounts[i] = 0;
            reindexSlot(i);
        }
        contentRevision++;
        changed.call();
    }

    /**
     * Set the logical (actual) count for a slot and update the display stack accordingly.
     * If template is non-null, its item/tag will be used for the display stack; otherwise the existing display stack is used.
     */
    public void setActualCountAndDisplay(int slot, int actual, ItemStack template) {
        if (slot < 0 || slot >= getSlots()) return;
        actualCounts[slot] = Math.max(0, Math.min(actual, HARD_MAX));
        if (actualCounts[slot] <= 0) {
            super.setStackInSlot(slot, ItemStack.EMPTY);
            return;
        }
        ItemStack disp;
        if (template != null && !template.isEmpty()) {
            disp = template.copy();
        } else {
            disp = getStackInSlot(slot).copy();
        }
        disp.setCount(Math.min(HARD_MAX, actualCounts[slot]));
        super.setStackInSlot(slot, disp);
    }
}
