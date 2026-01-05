package io.ticticboom.mods.mm.port.item;

import java.util.List;

import com.mojang.serialization.Codec;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class ItemPortHandler extends ItemStackHandler {

    public static Codec<List<ItemStack>> STACKS_CODEC = Codec.list(ItemStack.CODEC);
    private final INotifyChangeFunction changed;
    private final int slotCapacity; // 0 = use item default
    private static final int HARD_MAX = 1024;

    private final int[] actualCounts;

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
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    public Tag serializeStacks() {
        // store both display stacks and actualCounts into a compound
        var compound = new CompoundTag();
        var tag = NbtOps.INSTANCE.withEncoder(STACKS_CODEC).apply(stacks);
        compound.put("stacks", tag.getOrThrow(false, Ref.LOG::error));
        compound.putIntArray("counts", actualCounts);
        return compound;
    }

    public void deserializeStacks(Tag nbt) {
        if (!(nbt instanceof CompoundTag ct)) return;
        // read stacks
        Tag stacksTag = ct.get("stacks");
        if (stacksTag != null) {
            var res = NbtOps.INSTANCE.withDecoder(STACKS_CODEC).apply(stacksTag);
            var pair = res.getOrThrow(false, Ref.LOG::error);
            this.stacks.clear();
            List<ItemStack> list = pair.getFirst();
            for (int i = 0; i < list.size(); i++) {
                stacks.set(i, list.get(i));
            }
        }
        // read counts
        if (ct.contains("counts")) {
            int[] arr = ct.getIntArray("counts");
            int len = Math.min(arr.length, actualCounts.length);
            for (int i = 0; i < len; i++) {
                actualCounts[i] = arr[i];
            }
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        changed.call();
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStack stack = getStackInSlot(slot);
        // if no override configured, use item default or 64 fallback
        if (slotCapacity <= 0) {
            if (stack.isEmpty()) {
                return 64;
            }
            return stack.getItem().getMaxStackSize();
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
    public void setStackInSlot(int slot, ItemStack stack) {
        super.setStackInSlot(slot, stack);
        // set actual count to whatever the provided stack has (may be clamped)
        actualCounts[slot] = stack.getCount();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
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
                super.setStackInSlot(slot, placed);
                actualCounts[slot] = 1;
                if (working.isEmpty()) return ItemStack.EMPTY;
                return working;
            }
        }

        // If existing same item, try to add
        if (actualCounts[slot] > 0) {
            if (existing.getItem() != item) {
                return stack; // different item
            }
            // respect NBT: only merge when tags are equal or both null
            if (!areTagsEqualOrNull(existing.getTag(), stack.getTag())) {
                return stack; // different tag -> do not merge here
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
                int display = Math.min(existing.getMaxStackSize(), actualCounts[slot]);
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
            int display = Math.min(stack.getMaxStackSize(), toPlace);
            // preserve tag when placing
            ItemStack placed = stack.copy();
            placed.setCount(display);
            super.setStackInSlot(slot, placed);
            ItemStack res = stack.copy();
            res.shrink(toPlace);
            return res;
        }
    }

    public int canInsert(Item item, int count) {
        // If asking for a probe of available space, return total available space
        if (count == Integer.MAX_VALUE) {
            int available = 0;
            for (int slot = 0; slot < getSlots(); slot++) {
                ItemStack stack = getStackInSlot(slot);
                int limit = getSlotLimit(slot);
                if (stack.isEmpty()) {
                    available += limit;
                } else if (stack.getItem() == item) {
                    available += (limit - stack.getCount());
                }
            }
            return available;
        }
        // fallback: simulate insertion using ItemStack-based probe
        ItemStack probe = new ItemStack(item, count);
        int remaining = count;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (remaining <= 0) break;
            ItemStack res = insertItem(slot, probe.copy(), true);
            int moved = probe.getCount() - res.getCount();
            remaining -= moved;
            probe.setCount(res.getCount());
        }
        return remaining;
    }

    // New helper to probe canInsert for ItemStack (considers tag as separate identity when appropriate)
    public int canInsert(ItemStack stack, int count) {
        if (stack.isEmpty()) return count;
        if (count == Integer.MAX_VALUE) {
            int available = 0;
            for (int slot = 0; slot < getSlots(); slot++) {
                ItemStack s = getStackInSlot(slot);
                int limit = getSlotLimit(slot);
                if (s.isEmpty()) {
                    available += limit;
                } else if (s.getItem() == stack.getItem()) {
                    // if tags equal or one has no tag, we can merge; otherwise we treat as different and only empty slots work
                    if (areTagsEqualOrNull(s.getTag(), stack.getTag())) {
                        available += (limit - s.getCount());
                    }
                }
            }
            return available;
        }
        // fallback: try simulated insert using insertItem
        ItemStack probe = stack.copy();
        int remaining = count;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (remaining <= 0) break;
            ItemStack res = insertItem(slot, probe.copy(), true);
            int moved = probe.getCount() - res.getCount();
            remaining -= moved;
            probe.setCount(res.getCount());
        }
        return remaining;
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
        for (int slot = 0; slot < getSlots(); slot++) {
            if (remainingToInsert <= 0) break;
            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) continue;
            int limit = getSlotLimit(slot);
            int maxPerSlot = Math.max(1, limit);
            int toPlace = Math.min(maxPerSlot, remainingToInsert);
            actualCounts[slot] = toPlace;
            ItemStack placed;
            if (checkNbt) {
                placed = template.copy();
                placed.setCount(Math.min(placed.getMaxStackSize(), toPlace));
            } else {
                placed = new ItemStack(template.getItem(), Math.min(template.getItem().getMaxStackSize(), toPlace));
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
        for (int slot = 0; slot < getSlots(); slot++) {
            if (remainingToInsert <= 0) break;
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) continue;
            if (existing.getItem() != template.getItem()) continue;
            if (checkNbt && !areTagsEqualOrNull(existing.getTag(), template.getTag())) continue;
            int limit = getSlotLimit(slot);
            int space = limit - existing.getCount();
            if (space <= 0) continue;
            int toMove = Math.min(space, remainingToInsert);
            actualCounts[slot] = actualCounts[slot] + toMove;
            int display = Math.min(existing.getMaxStackSize(), actualCounts[slot]);
            ItemStack newDisplay = existing.copy();
            newDisplay.setCount(display);
            super.setStackInSlot(slot, newDisplay);
            remainingToInsert -= toMove;
        }
        return remainingToInsert;
    }

    private boolean areTagsEqualOrNull(CompoundTag a, CompoundTag b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
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
            int displayCount = Math.min(display.getMaxStackSize(), actualCounts[slot]);
            ItemStack newDisplay = display.copy();
            newDisplay.setCount(displayCount);
            super.setStackInSlot(slot, newDisplay);
        }
        ItemStack res = display.copy();
        res.setCount(toExtract);
        return res;
    }
}

