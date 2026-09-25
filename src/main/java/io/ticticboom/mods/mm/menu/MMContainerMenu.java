package io.ticticboom.mods.mm.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public abstract class MMContainerMenu extends AbstractContainerMenu {

    private final Block block;
    private final ContainerLevelAccess access;
    private final int storageSlots;

    protected MMContainerMenu(MenuType<?> type, Block block, int p_38852_, ContainerLevelAccess access, int storageSlots) {
        super(type, p_38852_);
        this.block = block;
        this.access = access;
        this.storageSlots = storageSlots;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int handlerIndex) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(handlerIndex);
        if (quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();

            int capSize = storageSlots;
            int pinvSize = 27;
            int phbSize = 9;
            int hbStart = capSize + pinvSize;
            int totalSize = capSize + pinvSize + phbSize;

            try {
                // when quick-moving into storage, prefer empty slots to avoid stacking onto existing
                io.ticticboom.mods.mm.port.item.ItemPortHandler.setThreadPreferEmpty(true);
                if (handlerIndex >= capSize && handlerIndex < totalSize) {
                    // moving from player inv to storage: item ports first top up stacks of the same item
                    // (up to their slot capacity), then fill empty slots; other storages only take empty slots
                    moveIntoItemPort(rawStack, capSize);
                    if (!rawStack.isEmpty() && !tryMoveToEmptySlots(rawStack, capSize)) {
                        if (handlerIndex < hbStart) {
                            if (!this.moveItemStackTo(rawStack, hbStart, totalSize, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!this.moveItemStackTo(rawStack, capSize, hbStart, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    // moving from storage to player/hotbar or other: use handler-first approach to avoid dupes
                    try {
                        // attempt to access underlying container for the source slot
                        Object contObj;
                        try {
                            java.lang.reflect.Field contField = quickMovedSlot.getClass().getDeclaredField("container");
                            contField.setAccessible(true);
                            contObj = contField.get(quickMovedSlot);
                        } catch (NoSuchFieldException nsf) {
                            contObj = quickMovedSlot.container;
                        }

                        if (contObj instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc) {
                            var handler = ipc.getHandler();

                            // Simulate extracting the full available amount
                            int available = handler.getActualCount(handlerIndex);
                            if (available <= 0) return ItemStack.EMPTY;
                            ItemStack simulated = handler.extractItem(handlerIndex, available, true);
                            if (simulated.isEmpty()) return ItemStack.EMPTY;

                            // Try to move the simulated stack into player inventory
                            ItemStack moving = simulated.copy();
                            int before = moving.getCount();
                            boolean movedAny = this.moveItemStackTo(moving, capSize, totalSize, false);
                            int after = moving.getCount();
                            int moved = before - after;
                            if (moved <= 0) {
                                return ItemStack.EMPTY;
                            }

                            // Now perform the actual extraction from the handler of the moved amount
                            handler.extractItem(handlerIndex, moved, false);
                            // Update source slot display
                            ItemStack newDisp = handler.getActualDisplayStack(handlerIndex);
                            quickMovedSlot.set(newDisp);
                        } else {
                            // fallback: use default behavior
                            if (!this.moveItemStackTo(rawStack, capSize, totalSize, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    } catch (Exception ignored) {
                        // fallback: default behavior
                        if (!this.moveItemStackTo(rawStack, capSize, totalSize, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                 }
            } finally {
                io.ticticboom.mods.mm.port.item.ItemPortHandler.setThreadPreferEmpty(false);
            }

            if (rawStack.isEmpty()) {
                quickMovedSlot.set(ItemStack.EMPTY);
            } else {
                quickMovedSlot.setChanged();
            }
            if (rawStack.getCount() == quickMovedStack.getCount()) {
                return ItemStack.EMPTY;
            }
            quickMovedSlot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, block);
    }

    /**
     * Inserts into the menu's item port through its handler, which merges into matching stacks first.
     */
    private void moveIntoItemPort(ItemStack source, int capSize) {
        if (capSize <= 0 || !(this.slots.get(0).container instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc)) {
            return;
        }
        io.ticticboom.mods.mm.port.item.ItemPortHandler.setThreadPreferEmpty(false);
        source.getCount();
        ItemStack rest = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(ipc.getHandler(), source.copy(), false);
        source.setCount(rest.getCount());
        rest.getCount();
    }

    private boolean tryMoveToEmptySlots(ItemStack source, int end) {
        boolean movedAny = false;
        for (int idx = 0; idx < end && !source.isEmpty(); idx++) {
            Slot dest = this.slots.get(idx);
            ItemStack destStack = dest.getItem();
            if (!destStack.isEmpty()) continue; // skip non-empty slots
            // determine how many we can place
            int limit = dest.getMaxStackSize();
            // If the destination slot's container is our ItemPortContainer, use its handler's slot limit.
            try {
                java.lang.reflect.Field contField = dest.getClass().getDeclaredField("container");
                contField.setAccessible(true);
                Object contObj = contField.get(dest);
                if (contObj instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc) {
                    limit = ipc.getHandler().getSlotLimit(idx);
                }
            } catch (NoSuchFieldException ignored) {
                // Some Slot implementations expose `container` as a public field; fallback to checking known container types
                if (dest.container instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc) {
                    limit = ipc.getHandler().getSlotLimit(idx);
                }
            } catch (Exception ignored) {
                // general fallback -> keep default
            }
            int toMove = Math.min(source.getCount(), limit);
            ItemStack moveStack = source.copy();
            moveStack.setCount(toMove);
            dest.set(moveStack);
            source.shrink(toMove);
            movedAny = true;
        }
        return movedAny;
    }
}
