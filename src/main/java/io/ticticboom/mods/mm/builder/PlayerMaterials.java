package io.ticticboom.mods.mm.builder;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Blocks for assembly come out of the player's inventory (nothing is needed in creative). */
public final class PlayerMaterials {
    private PlayerMaterials() {
    }

    public static boolean has(Player player, Block block) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Item item = block.asItem();
        if (item == Items.AIR) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes one block's item, preferring plain stacks so renamed or otherwise tagged items are used last.
     *
     * @return the taken item (empty in creative, where nothing is taken), or null when the player has none
     */
    public static @Nullable ItemStack take(Player player, Block block) {
        if (player.getAbilities().instabuild) {
            return ItemStack.EMPTY;
        }
        Item item = block.asItem();
        if (item == Items.AIR) {
            return null;
        }
        var items = player.getInventory().items;
        int tagged = -1;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.is(item)) {
                if (!stack.hasTag()) {
                    return takeOne(player, stack);
                }
                if (tagged < 0) {
                    tagged = i;
                }
            }
        }
        return tagged < 0 ? null : takeOne(player, items.get(tagged));
    }

    /** Gives back what {@link #take} returned. */
    public static void refund(Player player, ItemStack taken) {
        if (!taken.isEmpty()) {
            player.getInventory().placeItemBackInInventory(taken);
        }
    }

    private static ItemStack takeOne(Player player, ItemStack stack) {
        ItemStack taken = stack.split(1);
        player.getInventory().setChanged();
        return taken;
    }
}
