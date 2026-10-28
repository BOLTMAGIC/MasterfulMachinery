package io.ticticboom.mods.mm.builder;

import net.minecraft.world.item.ItemStack;

public interface ItemSink {
    void insert(ItemStack stack);

    boolean payEnergy(int fe);

    void refundEnergy(int fe);

    boolean gone();

    boolean ready();

    default void beginTick() {
    }

    boolean free();
}
