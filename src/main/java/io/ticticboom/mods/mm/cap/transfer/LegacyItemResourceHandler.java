package io.ticticboom.mods.mm.cap.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** An MM item handler as seen by NeoForge's transfer API: pipes, hoppers and other mods. */
public final class LegacyItemResourceHandler implements ResourceHandler<ItemResource> {
    private final IItemHandler handler;
    private final LegacyJournal journal;

    public LegacyItemResourceHandler(IItemHandler handler) {
        this.handler = handler;
        this.journal = new LegacyJournal(handler);
    }

    public IItemHandler handler() {
        return handler;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(handler.getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int limit = handler.getSlotLimit(index);
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && handler.isItemValid(index, resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        ItemStack stack = resource.toStack(amount);
        int accepted = amount - handler.insertItem(index, stack, true).getCount();
        if (accepted <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return accepted - handler.insertItem(index, resource.toStack(accepted), false).getCount();
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0 || !resource.matches(handler.getStackInSlot(index))) {
            return 0;
        }
        int available = handler.extractItem(index, amount, true).getCount();
        if (available <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return handler.extractItem(index, available, false).getCount();
    }
}
