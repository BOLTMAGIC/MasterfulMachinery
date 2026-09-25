package io.ticticboom.mods.mm.port.item;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A port slot in the GUI. Vanilla slots stop at the item's stack size (64), so items placed by hand
 * never stacked up to the port's slot capacity; this one uses the port's limit. Taking from it still
 * hands out at most one normal stack, so the cursor never holds an oversized stack.
 */
public class ItemPortSlot extends Slot {
    private final ItemPortHandler handler;
    private final boolean clientSide;

    /**
     * @param clientSide true for the client's copy of the menu
     */
    public ItemPortSlot(ItemPortContainer container, int index, int x, int y, boolean clientSide) {
        super(container, index, x, y);
        this.handler = container.getHandler();
        this.clientSide = clientSide;
    }

    /**
     * Vanilla sends slot contents with the count as a single byte, so counts above 127 arrive
     * wrapped. On the client the port's handler already gets the real counts from the port block
     * entity's own sync, so slot updates are ignored there instead of overwriting them.
     */
    @Override
    public void set(@NotNull ItemStack stack) {
        if (clientSide) {
            return;
        }
        super.set(stack);
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(getContainerSlot());
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        if (!stack.isStackable()) {
            return 1;
        }
        // exactly what the handler stores, so nothing placed here gets clamped away
        return handler.getSlotLimit(getContainerSlot());
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        ItemStack inSlot = getItem();
        int max = inSlot.isEmpty() ? amount : inSlot.getMaxStackSize();
        return super.remove(Math.min(amount, max));
    }
}
