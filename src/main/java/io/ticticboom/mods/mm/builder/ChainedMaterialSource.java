package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * The multiblock tool's source: blocks come from the tool's own store first, then the player's inventory;
 * energy comes from the tool. Nothing is taken or paid in creative. As a sink (dismantling) it stores items the
 * same way a refund does: tool store, then inventory, then the ground.
 */
public final class ChainedMaterialSource implements MaterialSource, ItemSink {
    private final ToolStore store;
    private final Player player;
    private final ToolEnergy energy;

    public ChainedMaterialSource(ToolStore store, Player player, ToolEnergy energy) {
        this.store = store;
        this.player = player;
        this.energy = energy;
    }

    @Override
    public boolean has(Block block) {
        if (free()) {
            return true;
        }
        return storeSlot(block) >= 0 || PlayerMaterials.has(player, block);
    }

    @Override
    public ItemStack take(Block block) {
        if (free()) {
            return ItemStack.EMPTY;
        }
        int slot = storeSlot(block);
        if (slot >= 0) {
            return store.extractItem(slot, 1, false);
        }
        ItemStack taken = PlayerMaterials.take(player, block);
        return taken == null ? ItemStack.EMPTY : taken;
    }

    @Override
    public void refund(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack leftover = stack;
        if (toolCarried()) {
            store.reload();
            leftover = ItemHandlerHelper.insertItemStacked(store, stack, false);
        }
        if (!leftover.isEmpty()) {
            // drops whatever the inventory cannot hold
            player.getInventory().placeItemBackInInventory(leftover);
        }
    }

    @Override
    public void insert(ItemStack stack) {
        refund(stack);
    }

    @Override
    public boolean payEnergy(int fe) {
        if (free() || fe <= 0) {
            return true;
        }
        if (!toolCarried() || energy.getEnergyStored() < fe) {
            return false;
        }
        energy.extractEnergy(fe, false);
        return true;
    }

    @Override
    public void refundEnergy(int fe) {
        if (free() || fe <= 0 || !toolCarried()) {
            return;
        }
        energy.restore(fe);
    }

    /** The tool left the player (dropped, stored away or split off); creative builds need nothing from it. */
    @Override
    public boolean gone() {
        return !free() && !toolCarried();
    }

    /** Waits while the tool's own store is open, so the menu and the job never write over each other. */
    @Override
    public boolean ready() {
        return !(player.containerMenu instanceof MultiblockToolMenu);
    }

    @Override
    public boolean free() {
        return player.getAbilities().instabuild;
    }

    /** The store slot to take block's item from (plain stacks before tagged ones, like the inventory), or -1. */
    private int storeSlot(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR || !toolCarried()) {
            return -1;
        }
        store.reload();
        int tagged = -1;
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack stack = store.getStackInSlot(i);
            if (stack.is(item)) {
                if (!stack.hasTag()) {
                    return i;
                }
                if (tagged < 0) {
                    tagged = i;
                }
            }
        }
        return tagged;
    }

    /** The tool still exists and is still on the player (not dropped, stored away or split off). */
    private boolean toolCarried() {
        if (!store.isToolStackPresent()) {
            return false;
        }
        ItemStack tool = store.toolStack();
        var inventory = player.getInventory();
        for (var compartment : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : compartment) {
                if (stack == tool) {
                    return true;
                }
            }
        }
        return false;
    }
}
