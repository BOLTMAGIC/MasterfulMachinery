package io.ticticboom.mods.mm.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A tool store slot in the menu. Mirrors {@link io.ticticboom.mods.mm.port.item.ItemPortSlot}: uses
 * the store's real limit instead of vanilla's 64-per-slot cap, ignores client-side {@code set()}
 * (the client already has the real count from the held tool's own NBT, synced by vanilla as part of
 * the held stack), and {@code remove()} hands out at most one normal stack so the cursor never holds
 * an oversized stack.
 * <p>
 * Also refuses pickup/place/remove outright once the store's bound tool stack is gone (e.g. the tool
 * was dropped or destroyed while the menu was open) - defense in depth alongside {@link ToolStore}'s
 * own guards and {@link MultiblockToolMenu#stillValid}.
 */
public class ToolSlot extends SlotItemHandler {
    private final ToolStore store;
    private final boolean clientSide;
    private final MultiblockToolMenu menu;

    /**
     * @param clientSide true for the client's copy of the menu
     */
    public ToolSlot(ToolStore store, int index, int x, int y, boolean clientSide, MultiblockToolMenu menu) {
        super(store, index, x, y);
        this.store = store;
        this.clientSide = clientSide;
        this.menu = menu;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        if (clientSide) {
            return;
        }
        super.set(stack);
    }

    /**
     * On the client, catch up with a fresh sync of the held stack before reading: vanilla replaces the
     * held {@link ItemStack} instance on resync, and {@link #set} above deliberately ignores those
     * pushes, so the store must instead be re-derived from whatever the client currently holds.
     */
    @Override
    public @NotNull ItemStack getItem() {
        if (clientSide) {
            menu.refreshClientStore();
        }
        return super.getItem();
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
        if (!store.isToolStackPresent()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = getItem();
        int max = inSlot.isEmpty() ? amount : inSlot.getMaxStackSize();
        return super.remove(Math.min(amount, max));
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return store.isToolStackPresent() && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return store.isToolStackPresent() && super.mayPlace(stack);
    }
}
