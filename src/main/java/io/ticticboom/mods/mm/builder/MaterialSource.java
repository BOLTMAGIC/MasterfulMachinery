package io.ticticboom.mods.mm.builder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Where an assembly job draws its blocks from: a player's inventory today, a multiblock tool's own store later. */
public interface MaterialSource {
    /** Can this source supply one of block's item (creative: always). */
    boolean has(Block block);

    /** Removes one item for block; EMPTY if none. Creative: returns EMPTY but succeeded() semantics via has(). */
    ItemStack take(Block block);

    /** Gives an item back (placement failed/cancelled). Must never delete it: store, inventory, or drop. */
    void refund(ItemStack stack);

    /** Pays energy for one block; true when paid (sources without energy always true). */
    default boolean payEnergy(int fe) {
        return true;
    }

    /** Creative: nothing is taken, nothing is paid. */
    boolean free();
}
