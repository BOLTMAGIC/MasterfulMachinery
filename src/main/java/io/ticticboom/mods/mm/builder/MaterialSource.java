package io.ticticboom.mods.mm.builder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public interface MaterialSource {
    boolean has(Block block);

    ItemStack take(Block block);

    void refund(ItemStack stack);

    default boolean payEnergy(int fe) {
        return true;
    }

    default void refundEnergy(int fe) {
    }

    default boolean gone() {
        return false;
    }

    default boolean ready() {
        return true;
    }

    default void beginTick() {
    }

    default @Nullable Component summaryNote() {
        return null;
    }

    boolean free();
}
