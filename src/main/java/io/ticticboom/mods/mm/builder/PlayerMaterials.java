package io.ticticboom.mods.mm.builder;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

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

    public static boolean take(Player player, Block block) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Item item = block.asItem();
        if (item == Items.AIR) {
            return false;
        }
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.is(item)) {
                stack.shrink(1);
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }
}
