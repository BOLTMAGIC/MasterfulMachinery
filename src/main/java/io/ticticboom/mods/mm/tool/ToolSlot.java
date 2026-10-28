package io.ticticboom.mods.mm.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ToolSlot extends SlotItemHandler {
    private final ToolStore store;
    private final boolean clientSide;
    private final MultiblockToolMenu menu;

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
