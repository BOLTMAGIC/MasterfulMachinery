package io.ticticboom.mods.mm.port.item;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
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
        actualCounts[slot] = stack == null ? 0 : stack.getCount();
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
                super.setStackInSlot(slot, new ItemStack(item, 1));
                actualCounts[slot] = 1;
                return stack.copy().split(1);
            }
        }

        // If existing same item, try to add
        if (actualCounts[slot] > 0) {
            if (existing.getItem() != item) {
                return stack; // different item
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
                super.setStackInSlot(slot, new ItemStack(item, display));
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
            super.setStackInSlot(slot, new ItemStack(item, display));
            ItemStack res = stack.copy();
            res.shrink(toPlace);
            return res;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;
        if (actualCounts[slot] <= 0) return ItemStack.EMPTY;
        ItemStack display = getStackInSlot(slot);
        Item item = display.getItem();
        int toExtract = Math.min(amount, actualCounts[slot]);
        if (simulate) {
            return new ItemStack(item, toExtract);
        }
        // perform removal
        actualCounts[slot] -= toExtract;
        if (actualCounts[slot] <= 0) {
            super.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            int displayCount = Math.min(display.getMaxStackSize(), actualCounts[slot]);
            super.setStackInSlot(slot, new ItemStack(item, displayCount));
        }
        return new ItemStack(item, toExtract);
    }
}

