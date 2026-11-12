package io.ticticboom.mods.mm.tool;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI's "+" on a structure page while the Structure Builder's store is open: moves what the structure needs from the
 * player's inventory (main slots and offhand, never armor) into the store. What the store already holds counts, so only
 * the rest is taken; what doesn't fit or isn't carried stays where it is.
 */
public final class ToolStoreFill {
    /** Limits on what a client may send. */
    public static final int MAX_NEEDS = 256;
    public static final int MAX_OPTIONS = 64;
    public static final int MAX_COUNT = 1_000_000;

    private ToolStoreFill() {
    }

    /**
     * One kind of block the structure needs: any of options (a port of any tier, say), count of them in all.
     */
    public record Need(List<Item> options, int count) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(options.size());
            options.forEach(item -> buf.writeId(BuiltInRegistries.ITEM, item));
            buf.writeVarInt(count);
        }

        public static Need read(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_OPTIONS) {
                throw new IllegalArgumentException("too many options: " + size);
            }
            var options = new ArrayList<Item>(size);
            for (int i = 0; i < size; i++) {
                Item item = buf.readById(BuiltInRegistries.ITEM);
                if (item != null && item != Items.AIR) {
                    options.add(item);
                }
            }
            return new Need(List.copyOf(options), Math.max(0, Math.min(buf.readVarInt(), MAX_COUNT)));
        }
    }

    /** @return how many items were moved */
    public static int fill(Player player, ToolStore store, List<Need> needs) {
        int moved = 0;
        for (Need need : needs) {
            int want = need.count() - stored(store, need.options());
            moved += take(player.getInventory().items, store, need.options(), want);
            want = need.count() - stored(store, need.options());
            moved += take(player.getInventory().offhand, store, need.options(), want);
        }
        if (moved > 0) {
            player.getInventory().setChanged();
        }
        return moved;
    }

    /** How many of options the player carries that the store lacks: what {@link #fill} would move at most. */
    public static int movable(Player player, ToolStore store, List<Need> needs) {
        int movable = 0;
        for (Need need : needs) {
            int want = Math.max(0, need.count() - stored(store, need.options()));
            int carried = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && need.options().contains(stack.getItem())) carried += stack.getCount();
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (!stack.isEmpty() && need.options().contains(stack.getItem())) carried += stack.getCount();
            }
            movable += Math.min(want, carried);
        }
        return movable;
    }

    private static int take(NonNullList<ItemStack> slots, ToolStore store, List<Item> options, int want) {
        int moved = 0;
        for (int slot = 0; slot < slots.size() && want > 0; slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty() || !options.contains(stack.getItem())) {
                continue;
            }
            ItemStack offer = stack.copyWithCount(Math.min(want, stack.getCount()));
            ItemStack rest = ItemHandlerHelper.insertItemStacked(store, offer, false);
            int taken = offer.getCount() - rest.getCount();
            stack.shrink(taken);
            want -= taken;
            moved += taken;
            if (!rest.isEmpty()) {
                // the store takes no more of this (full, or the item is never stored)
                break;
            }
        }
        return moved;
    }

    private static int stored(ToolStore store, List<Item> options) {
        int stored = 0;
        for (int slot = 0; slot < store.getSlots(); slot++) {
            ItemStack stack = store.getStackInSlot(slot);
            if (!stack.isEmpty() && options.contains(stack.getItem())) {
                stored += stack.getCount();
            }
        }
        return stored;
    }
}
