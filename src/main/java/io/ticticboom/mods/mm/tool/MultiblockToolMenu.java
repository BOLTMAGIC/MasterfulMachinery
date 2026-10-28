package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.setup.MMRegisters;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

public class MultiblockToolMenu extends AbstractContainerMenu {
    public static final int STORE_SLOTS = ToolStore.SLOTS;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;
    private static final int OFFHAND_INV_INDEX = 40;

    private final Player player;
    @Getter
    private final InteractionHand hand;
    @Getter
    private final ToolStore store;
    @Getter
    private final int lockedSlotIndex;
    private final int lockedInventoryIndex;
    private ItemStack toolStack;

    public MultiblockToolMenu(int windowId, Inventory inv, InteractionHand hand) {
        super(MMRegisters.MULTIBLOCK_TOOL_MENU.get(), windowId);
        this.player = inv.player;
        this.hand = hand;
        this.toolStack = player.getItemInHand(hand);
        this.store = new ToolStore(toolStack, player.level().registryAccess());

        boolean clientSide = player.level().isClientSide();
        for (int i = 0; i < STORE_SLOTS; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            addSlot(new ToolSlot(store, i, x, y, clientSide, this));
        }

        int locked = -1;
        int lockedInvIndex = -1;
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                int invIndex = 9 + row * PLAYER_INV_COLS + col;
                addSlot(new Slot(inv, invIndex, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            boolean isHeldSlot = hand == InteractionHand.MAIN_HAND && col == inv.getSelectedSlot();
            int x = 8 + col * 18;
            addSlot(isHeldSlot ? new LockedSlot(inv, col, x, 198) : new Slot(inv, col, x, 198));
            if (isHeldSlot) {
                locked = STORE_SLOTS + PLAYER_INV_ROWS * PLAYER_INV_COLS + col;
                lockedInvIndex = col;
            }
        }
        if (hand == InteractionHand.OFF_HAND) {
            locked = slots.size();
            lockedInvIndex = OFFHAND_INV_INDEX;
            addSlot(new LockedSlot(inv, OFFHAND_INV_INDEX, 8 + PLAYER_INV_COLS * 18 + 12, 198));
        }
        this.lockedSlotIndex = locked;
        this.lockedInventoryIndex = lockedInvIndex;
    }

    public MultiblockToolMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(windowId, inv, buf.readEnum(InteractionHand.class));
    }

    @Override
    public boolean stillValid(@NotNull Player checkPlayer) {
        return checkPlayer == player && isToolStackValid();
    }

    private boolean isToolStackValid() {
        return !toolStack.isEmpty() && toolStack.getItem() instanceof MultiblockToolItem
                && player.getItemInHand(hand) == toolStack;
    }

    void refreshClientStore() {
        if (!player.level().isClientSide()) {
            return;
        }
        ItemStack current = player.getItemInHand(hand);
        if (current != toolStack && !current.isEmpty()) {
            toolStack = current;
            store.rebind(current);
        }
    }

    public void moveSlot(int index, int x, int y) {
        if (!player.level().isClientSide()) {
            return;
        }
        Slot old = this.slots.get(index);
        Slot moved;
        if (old instanceof ToolSlot) {
            moved = new ToolSlot(store, old.getContainerSlot(), x, y, true, this);
        } else if (old instanceof LockedSlot) {
            moved = new LockedSlot(player.getInventory(), old.getContainerSlot(), x, y);
        } else {
            moved = new Slot(old.container, old.getContainerSlot(), x, y);
        }
        moved.index = old.index;
        this.slots.set(index, moved);
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ContainerInput clickType, @NotNull Player clicker) {
        refreshClientStore();
        if (!isToolStackValid()) {
            return;
        }
        if (clickType == ContainerInput.SWAP) {
            handleSwap(slotId, button, clicker);
            return;
        }
        if (clickType == ContainerInput.PICKUP && slotId >= 0 && slotId < STORE_SLOTS) {
            Slot slot = this.slots.get(slotId);
            ItemStack slotStack = slot.getItem();
            ItemStack carried = getCarried();
            if (!slotStack.isEmpty() && !carried.isEmpty()
                    && !ItemStack.isSameItemSameComponents(slotStack, carried)
                    && slotStack.getCount() > slotStack.getMaxStackSize()) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, clicker);
    }

    private void handleSwap(int slotId, int button, Player clicker) {
        if ((button < 0 || button > 8) && button != OFFHAND_INV_INDEX) {
            return;
        }
        if (isLockedInventoryIndex(button) || slotId == lockedSlotIndex) {
            return;
        }
        if (slotId < 0 || slotId >= STORE_SLOTS) {
            super.clicked(slotId, button, ContainerInput.SWAP, clicker);
            return;
        }

        Slot fromSlot = this.slots.get(slotId);
        ItemStack fromStack = fromSlot.getItem();
        Inventory inv = clicker.getInventory();
        ItemStack destStack = inv.getItem(button);
        if (fromStack.isEmpty() && destStack.isEmpty()) {
            return;
        }

        if (fromStack.isEmpty() || ItemStack.isSameItemSameComponents(fromStack, destStack)) {
            if (destStack.isEmpty()) {
                return;
            }
            ItemStack leftover = store.insertItem(slotId, destStack.copy(), false);
            if (leftover.getCount() == destStack.getCount()) {
                return;
            }
            inv.setItem(button, leftover);
            fromSlot.setChanged();
            return;
        }

        if (!destStack.isEmpty()) {
            return;
        }

        int normalMax = fromStack.getMaxStackSize();
        ItemStack moving = fromSlot.remove(Math.min(fromStack.getCount(), normalMax));
        if (moving.isEmpty()) {
            return;
        }
        inv.setItem(button, moving);
        fromSlot.setChanged();
    }

    private boolean isLockedInventoryIndex(int inventoryIndex) {
        return lockedInventoryIndex >= 0 && inventoryIndex == lockedInventoryIndex;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player mover, int index) {
        refreshClientStore();
        if (!isToolStackValid() || index == lockedSlotIndex) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceSlot.getItem().copy();

        if (index < STORE_SLOTS) {
            ItemStack extracted = sourceSlot.remove(original.getCount());
            if (!extracted.isEmpty()) {
                ItemStack moving = extracted.copy();
                moveItemStackTo(moving, STORE_SLOTS, this.slots.size(), true);
                if (!moving.isEmpty()) {
                    ItemStack backLeftover = store.insertItem(index, moving, false);
                    returnLeftoverSafely(mover, backLeftover);
                }
                sourceSlot.setChanged();
                sourceSlot.onTake(mover, extracted);
            }
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(store, sourceStack.copy(), false);
        sourceStack.setCount(remainder.getCount());
        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        ItemStack after = sourceSlot.getItem();
        if (after.getCount() == original.getCount() && ItemStack.isSameItemSameComponents(after, original)) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(mover, after);
        return original;
    }

    private void returnLeftoverSafely(Player player, ItemStack leftover) {
        if (leftover.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(leftover)) {
            player.drop(leftover, false);
        }
    }

    private static final class LockedSlot extends Slot {
        LockedSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
