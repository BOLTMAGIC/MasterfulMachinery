package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.port.item.ItemPortContainer;
import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SingleMachineItemSlot extends Slot {

    private final ItemPortHandler handler;
    private final boolean input;

    public SingleMachineItemSlot(ItemPortContainer container, int index, int x, int y, boolean input) {
        super(container, index, x, y);
        this.handler = container.getHandler();
        this.input = input;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return input;
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(getContainerSlot());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!stack.isStackable()) {
            return 1;
        }
        return handler.getSlotLimit(getContainerSlot());
    }
}
