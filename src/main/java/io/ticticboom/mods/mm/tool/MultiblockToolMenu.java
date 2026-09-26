package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.setup.MMRegisters;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

/**
 * The Multiblock Tool's own menu: a 54-slot, 512-per-slot block store backed by the held tool's own
 * NBT, plus the player's inventory. The held tool's own slot is locked (can't be clicked, swapped with
 * a number key, or quick-moved) so the tool can never end up stored inside, or swapped out from under,
 * its own store. Closes automatically ({@link #stillValid}) if the held item stops being that same
 * tool stack.
 * <p>
 * Store slots can legitimately hold far more than a normal stack (up to {@link ToolStore#LIMIT}), so
 * several of vanilla's click paths would otherwise hand out or store an oversized stack raw (bypassing
 * {@link ToolSlot#remove}), which then loses count on save (world NBT saves {@code Count} as a byte).
 * {@link #clicked} intercepts {@link ClickType#PICKUP} and {@link ClickType#SWAP} on store slots to
 * keep every transfer capped at one normal stack; {@link #quickMoveStack} does the same for shift-click.
 */
public class MultiblockToolMenu extends AbstractContainerMenu {
    public static final int STORE_SLOTS = ToolStore.SLOTS;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;
    /** Vanilla's own {@code Inventory} index for the offhand slot (see {@code Inventory#getItem}). */
    private static final int OFFHAND_INV_INDEX = 40;

    private final Player player;
    private final InteractionHand hand;
    @Getter
    private final ToolStore store;
    /** Container index of the currently held tool's own slot; that slot is locked. */
    @Getter
    private final int lockedSlotIndex;
    /** Vanilla inventory index (0-8 hotbar, or 40 offhand) backing {@link #lockedSlotIndex}. */
    private final int lockedInventoryIndex;
    /**
     * The stack this menu was opened with. Reassigned only on the client ({@link #refreshClientStore})
     * when vanilla replaces the held stack instance on resync; the server's copy never changes after
     * construction, so {@link #stillValid} stays authoritative.
     */
    private ItemStack toolStack;

    public MultiblockToolMenu(int windowId, Inventory inv, InteractionHand hand) {
        super(MMRegisters.MULTIBLOCK_TOOL_MENU.get(), windowId);
        this.player = inv.player;
        this.hand = hand;
        this.toolStack = player.getItemInHand(hand);
        this.store = new ToolStore(toolStack);

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
            boolean isHeldSlot = hand == InteractionHand.MAIN_HAND && col == inv.selected;
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

    public MultiblockToolMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readEnum(InteractionHand.class));
    }

    @Override
    public boolean stillValid(@NotNull Player checkPlayer) {
        return checkPlayer == player && player.getItemInHand(hand) == toolStack;
    }

    /**
     * Client-only: vanilla resyncs the held stack by handing our locked slot a brand-new
     * {@link ItemStack} instance (see {@link ToolSlot#set}, which lets that through since it's a plain
     * {@link LockedSlot}, not a {@link ToolSlot}). Catches that up by re-deriving the store's contents
     * from whichever instance is now actually held, without ever touching server state.
     */
    void refreshClientStore() {
        if (!player.level().isClientSide()) {
            return;
        }
        ItemStack current = player.getItemInHand(hand);
        if (current != toolStack) {
            toolStack = current;
            store.rebind(current);
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player clicker) {
        if (clickType == ClickType.SWAP) {
            handleSwap(slotId, button, clicker);
            return;
        }
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < STORE_SLOTS) {
            Slot slot = this.slots.get(slotId);
            ItemStack slotStack = slot.getItem();
            ItemStack carried = getCarried();
            if (!slotStack.isEmpty() && !carried.isEmpty()
                    && !ItemStack.isSameItemSameTags(slotStack, carried)
                    && slotStack.getCount() > slotStack.getMaxStackSize()) {
                // vanilla would swap the two raw (cursor <- slot.getItem()), handing out the full
                // oversized stack uncapped; refuse the whole click instead of allowing that
                return;
            }
        }
        super.clicked(slotId, button, clickType, clicker);
    }

    /**
     * Handles {@link ClickType#SWAP} (number-key / offhand-key) fully ourselves rather than delegating
     * to vanilla, which would otherwise write a store slot's raw (possibly oversized) stack straight
     * into a hotbar/offhand slot via {@code Inventory#setItem}, bypassing {@link ToolSlot#remove}.
     * Also refuses any swap whose destination is the held tool's own slot, from any source slot.
     */
    private void handleSwap(int slotId, int button, Player clicker) {
        if (isLockedInventoryIndex(button) || slotId == lockedSlotIndex) {
            return;
        }
        if (slotId < 0 || slotId >= STORE_SLOTS) {
            super.clicked(slotId, button, ClickType.SWAP, clicker);
            return;
        }

        Slot fromSlot = this.slots.get(slotId);
        ItemStack fromStack = fromSlot.getItem();
        if (fromStack.isEmpty()) {
            return;
        }
        Inventory inv = clicker.getInventory();
        ItemStack destStack = inv.getItem(button);
        if (!destStack.isEmpty() && !ItemStack.isSameItemSameTags(fromStack, destStack)) {
            // a raw swap here would hand the destination the full (possibly oversized) store stack
            return;
        }

        int normalMax = fromStack.getMaxStackSize();
        int space = destStack.isEmpty() ? normalMax : normalMax - destStack.getCount();
        if (space <= 0) {
            return;
        }
        ItemStack moving = fromSlot.remove(Math.min(fromStack.getCount(), space));
        if (moving.isEmpty()) {
            return;
        }
        if (destStack.isEmpty()) {
            inv.setItem(button, moving);
        } else {
            destStack.grow(moving.getCount());
        }
        fromSlot.setChanged();
    }

    private boolean isLockedInventoryIndex(int inventoryIndex) {
        return lockedInventoryIndex >= 0 && inventoryIndex == lockedInventoryIndex;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player mover, int index) {
        if (index == lockedSlotIndex) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = sourceSlot.getItem().copy();

        if (index < STORE_SLOTS) {
            // tool store -> player inventory: hand out at most one normal stack, ever, per click
            ItemStack extracted = sourceSlot.remove(original.getCount());
            if (!extracted.isEmpty()) {
                ItemStack moving = extracted.copy();
                moveItemStackTo(moving, STORE_SLOTS, this.slots.size(), true);
                if (!moving.isEmpty()) {
                    // couldn't place it all elsewhere: put the rest back in the same slot, and if even
                    // that somehow doesn't fully fit, never silently drop it - hand it to the player
                    ItemStack backLeftover = store.insertItem(index, moving, false);
                    returnLeftoverSafely(mover, backLeftover);
                }
                sourceSlot.setChanged();
                sourceSlot.onTake(mover, extracted);
            }
            // always stop here: vanilla's shift-click loop keeps calling quickMoveStack for the same
            // slot as long as it gets back a non-empty stack, which would drain the whole 512 in one
            // shift-click; returning EMPTY caps it at the single stack already moved above
            return ItemStack.EMPTY;
        }

        // player inventory -> tool store: merge into existing stacks first, then empty slots
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(store, sourceStack.copy(), false);
        sourceStack.setCount(remainder.getCount());
        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        ItemStack after = sourceSlot.getItem();
        if (after.getCount() == original.getCount() && ItemStack.isSameItemSameTags(after, original)) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(mover, after);
        return original;
    }

    /** Never silently destroy items that couldn't be placed back where they came from. */
    private void returnLeftoverSafely(Player player, ItemStack leftover) {
        if (leftover.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(leftover)) {
            player.drop(leftover, false);
        }
    }

    /**
     * A player-inventory slot that refuses direct pickup/place. Used for the held tool's own slot so
     * it can't be taken, swapped in a number-key hotbar swap, or dragged over, while the menu is open.
     */
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
