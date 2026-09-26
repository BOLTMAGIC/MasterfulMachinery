package io.ticticboom.mods.mm.tool;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A tool store slot in the menu. Mirrors {@link io.ticticboom.mods.mm.port.item.ItemPortSlot}: uses
 * the store's real limit instead of vanilla's 64-per-slot cap, ignores client-side {@code set()}
 * (the client already has the real count from the held tool's own NBT, synced by vanilla as part of
 * the held stack), and {@code remove()} hands out at most one normal stack so the cursor never holds
 * an oversized stack.
 */
public class ToolSlot extends SlotItemHandler {
    private final ToolStore store;
    private final boolean clientSide;

    /**
     * @param clientSide true for the client's copy of the menu
     */
    public ToolSlot(ToolStore store, int index, int x, int y, boolean clientSide) {
        super(store, index, x, y);
        this.store = store;
        this.clientSide = clientSide;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        if (clientSide) {
            return;
        }
        super.set(stack);
    }

    @Override
    public int getMaxStackSize() {
        return store.getSlotLimit(getContainerSlot());
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        if (!stack.isStackable()) {
            return 1;
        }
        return store.getSlotLimit(getContainerSlot());
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        ItemStack inSlot = getItem();
        int max = inSlot.isEmpty() ? amount : inSlot.getMaxStackSize();
        return super.remove(Math.min(amount, max));
    }
}
