package io.ticticboom.mods.mm.tool;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ToolStore extends ItemStackHandler {
    public static final int SLOTS = 54;
    public static final int LIMIT = 512;
    private static final String COUNT_KEY = "C";

    private final HolderLookup.Provider registries;
    private ItemStack toolStack;

    public ToolStore(ItemStack toolStack, HolderLookup.Provider registries) {
        super(SLOTS);
        this.registries = registries;
        this.toolStack = toolStack;
        read(toolStack);
    }

    public void rebind(ItemStack newToolStack) {
        if (newToolStack.isEmpty()) {
            return;
        }
        this.toolStack = newToolStack;
        read(newToolStack);
    }

    public void reload() {
        if (toolStack.isEmpty()) {
            return;
        }
        read(toolStack);
    }

    private void read(ItemStack stack) {
        deserializeNBT(registries, ToolComponents.read(stack, ToolComponents.STORE));
    }

    public ItemStack toolStack() {
        return toolStack;
    }

    public boolean isToolStackPresent() {
        return !toolStack.isEmpty();
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (toolStack.isEmpty()) {
            return;
        }
        toolStack.set(ToolComponents.STORE.get(), CustomData.of(serializeNBT(registries)));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isToolStackPresent()) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isToolStackPresent()) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return LIMIT;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return stack.isStackable() ? LIMIT : 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !(stack.getItem() instanceof MultiblockToolItem)
                && !stack.has(DataComponents.BLOCK_ENTITY_DATA)
                && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && stack.getCapability(Capabilities.ItemHandler.ITEM) == null;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var nbt = new CompoundTag();
        var list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            var itemTag = (CompoundTag) stack.copyWithCount(1).save(provider);
            itemTag.putInt("Slot", i);
            itemTag.putInt(COUNT_KEY, stack.getCount());
            list.add(itemTag);
        }
        nbt.put("Items", list);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        setSize(nbt.contains("Size") ? nbt.getIntOr("Size", 0) : SLOTS);
        ListTag items = nbt.getListOrEmpty("Items");
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompoundOrEmpty(i);
            int slot = itemTag.getIntOr("Slot", 0);
            if (slot < 0 || slot >= stacks.size()) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(provider, itemTag);
            if (!stack.isEmpty() && itemTag.contains(COUNT_KEY)) {
                stack.setCount(itemTag.getIntOr(COUNT_KEY, 0));
            }
            stacks.set(slot, stack);
        }
        onLoad();
    }
}
