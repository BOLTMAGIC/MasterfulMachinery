package io.ticticboom.mods.mm.builder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

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
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (buildable(stack, item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean buildable(ItemStack stack, Item item) {
        return stack.is(item) && !stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }

    public static @Nullable ItemStack take(Player player, Block block) {
        if (player.getAbilities().instabuild) {
            return ItemStack.EMPTY;
        }
        Item item = block.asItem();
        if (item == Items.AIR) {
            return null;
        }
        var items = player.getInventory().getNonEquipmentItems();
        int tagged = -1;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (buildable(stack, item)) {
                if (stack.getComponentsPatch().isEmpty()) {
                    return takeOne(player, stack);
                }
                if (tagged < 0) {
                    tagged = i;
                }
            }
        }
        return tagged < 0 ? null : takeOne(player, items.get(tagged));
    }

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
