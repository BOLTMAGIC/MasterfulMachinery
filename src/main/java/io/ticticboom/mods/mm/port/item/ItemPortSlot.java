package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.port.common.PortPager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemPortSlot extends Slot {

    private final ItemPortHandler handler;
    private final int handlerIndex;
    private final PortPager pager;
    private final int page;

    public ItemPortSlot(ItemPortContainer container, int index, int x, int y, PortPager pager, int page) {
        super(container, index, x, y);
        this.handler = container.getHandler();
        this.handlerIndex = index;
        this.pager = pager;
        this.page = page;
    }

    @Override
    public boolean isActive() {
        return pager.page() == page;
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(handlerIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!stack.isStackable()) {
            return 1;
        }
        return handler.getSlotLimit(handlerIndex);
    }
}
