package io.ticticboom.mods.mm.builder;

import net.minecraft.world.item.ItemStack;

/** Where a dismantle job puts the blocks it breaks, and what pays for breaking them. */
public interface ItemSink {
    /** Stores a dropped item. Must never delete it: tool store, inventory, or drop at the player. */
    void insert(ItemStack stack);

    /** Pays energy for one block; true when paid. */
    boolean payEnergy(int fe);

    /** Gives back energy paid for a block that could not be broken after all. */
    void refundEnergy(int fe);

    /** True once the sink itself is gone (e.g. the tool was dropped); the job then stops. */
    boolean gone();

    /** False while the sink cannot be written safely right now (the job waits, e.g. the tool's store is open). */
    boolean ready();

    /** Called once per job tick, after {@link #ready()} and before any other call (see {@link MaterialSource#beginTick}). */
    default void beginTick() {
    }

    /** Creative: nothing is paid. */
    boolean free();
}
