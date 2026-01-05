package io.ticticboom.mods.mm.port.item;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemPortContainer implements Container {

    private final ItemPortHandler handler;

    public ItemPortContainer(ItemPortHandler handler) {
        this.handler = handler;
    }

    public ItemPortHandler getHandler() {
        return this.handler;
    }

    @Override
    public int getContainerSize() {
        return handler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getActualCount(i) > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
       return handler.getActualDisplayStack(i);
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        // Use handler.extractItem to remove from the logical storage (respects actualCounts)
        ItemStack res = handler.extractItem(i, i1, false);
        if (!res.isEmpty()) {
            this.setChanged();
        }
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        // Remove without notifying change: extract the full actual count and return the stack
        int actual = handler.getActualCount(i);
        if (actual <= 0) return ItemStack.EMPTY;
        ItemStack display = handler.getStackInSlot(i);
        ItemStack res = display.copy();
        res.setCount(actual);
        // clear internal storage without calling change notifier
        handler.setActualCountAndDisplay(i, 0, ItemStack.EMPTY);
        return res;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        // Use handler API to set logical count and display appropriately
        int limit = handler.getSlotLimit(i);
        int toSet = Math.min(limit, itemStack.isEmpty() ? 0 : itemStack.getCount());
        handler.setActualCountAndDisplay(i, toSet, itemStack);
        this.setChanged();
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        handler.getStacks().clear();
    }
}
