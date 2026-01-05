package io.ticticboom.mods.mm.menu;

import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class MMContainerMenu extends AbstractContainerMenu {

    private final MenuType<?> type;
    private final Block block;
    private final ContainerLevelAccess access;
    private final int storageSlots;

    protected MMContainerMenu(MenuType<?> type, Block block, int p_38852_, ContainerLevelAccess access, int storageSlots) {
        super(type, p_38852_);
        this.type = type;
        this.block = block;
        this.access = access;
        this.storageSlots = storageSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(i);
        if (quickMovedSlot != null && quickMovedSlot.hasItem()) {
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
                if (i >= capSize && i < totalSize) {
                    // moving from player inv to storage: prefer empty slots in storage range [0, capSize)
                    if (!tryMoveToEmptySlots(rawStack, 0, capSize)) {
                        if (i < hbStart) {
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
                        Object contObj = null;
                        try {
                            java.lang.reflect.Field contField = quickMovedSlot.getClass().getDeclaredField("container");
                            contField.setAccessible(true);
                            contObj = contField.get(quickMovedSlot);
                        } catch (NoSuchFieldException nsf) {
                            contObj = quickMovedSlot.container;
                        }

                        if (contObj instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc) {
                            var handler = ipc.getHandler();
                            int handlerIndex = i; // storage slots are added first in setup

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
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, block);
    }

    private boolean tryMoveToEmptySlots(ItemStack source, int start, int end) {
        boolean movedAny = false;
        for (int idx = start; idx < end && !source.isEmpty(); idx++) {
            Slot dest = this.slots.get(idx);
            if (dest == null) continue;
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
                    limit = ipc.getHandler().getSlotLimit(idx - start);
                }
            } catch (NoSuchFieldException ignored) {
                // Some Slot implementations expose `container` as a public field; fallback to checking known container types
                if (dest.container instanceof io.ticticboom.mods.mm.port.item.ItemPortContainer ipc) {
                    limit = ipc.getHandler().getSlotLimit(idx - start);
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
