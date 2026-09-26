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

    /** Gives back energy paid for a block that could not be placed after all. */
    default void refundEnergy(int fe) {
    }

    /** True once the source itself is gone (e.g. the tool was dropped); the job then stops. */
    default boolean gone() {
        return false;
    }

    /** False while the source cannot be drawn from safely right now (the job waits, e.g. the tool's store is open). */
    default boolean ready() {
        return true;
    }

    /** Creative: nothing is taken, nothing is paid. */
    boolean free();
}
