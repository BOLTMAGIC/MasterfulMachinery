package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.setup.MMRegisters;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 */
public class MultiblockToolMenu extends AbstractContainerMenu {
    public static final int STORE_SLOTS = ToolStore.SLOTS;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack toolStack;
    @Getter
    private final ToolStore store;
    /** Container index of the currently held tool's own slot; that slot is locked. */
    @Getter
    private final int lockedSlotIndex;

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
            addSlot(new ToolSlot(store, i, x, y, clientSide));
        }

        int locked = -1;
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                int invIndex = 9 + row * PLAYER_INV_COLS + col;
                addSlot(new Slot(inv, invIndex, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
            if (hand == InteractionHand.MAIN_HAND && col == inv.selected) {
                locked = STORE_SLOTS + PLAYER_INV_ROWS * PLAYER_INV_COLS + col;
            }
        }
        if (hand == InteractionHand.OFF_HAND) {
            // vanilla's own inventory index for the offhand slot
            locked = slots.size();
            addSlot(new Slot(inv, 40, 8 + PLAYER_INV_COLS * 18 + 12, 198));
        }
        this.lockedSlotIndex = locked;
    }

    public MultiblockToolMenu(int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(windowId, inv, buf.readEnum(InteractionHand.class));
    }

    @Override
    public boolean stillValid(@NotNull Player checkPlayer) {
        return checkPlayer == player && player.getItemInHand(hand) == toolStack;
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
            // tool store -> player inventory: hand out at most one normal stack per click
            ItemStack extracted = sourceSlot.remove(original.getCount());
            if (!extracted.isEmpty()) {
                ItemStack moving = extracted.copy();
                moveItemStackTo(moving, STORE_SLOTS, this.slots.size(), true);
                if (!moving.isEmpty()) {
                    // couldn't place it all elsewhere: put the rest back in the same slot
                    store.insertItem(index, moving, false);
                }
                sourceSlot.setChanged();
            }
        } else {
            // player inventory -> tool store: merge into existing stacks first, then empty slots
            ItemStack sourceStack = sourceSlot.getItem();
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(store, sourceStack.copy(), false);
            sourceStack.setCount(remainder.getCount());
            if (sourceStack.isEmpty()) {
                sourceSlot.set(ItemStack.EMPTY);
            } else {
                sourceSlot.setChanged();
            }
        }

        ItemStack after = sourceSlot.getItem();
        if (after.getCount() == original.getCount() && ItemStack.isSameItemSameTags(after, original)) {
            return ItemStack.EMPTY;
        }
        sourceSlot.onTake(mover, after);
        return original;
    }
}
