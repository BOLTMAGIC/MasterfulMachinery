package io.ticticboom.mods.mm.builder.me;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface MeAccess {
    boolean reachable();

    default void refresh() {
    }

    long stock(Item item);

    ItemStack extract(Item item, int amount, boolean simulate);

    boolean isCraftable(Item item);

    default long requestedAmount(Item item) {
        return 0;
    }

    CraftHandle requestCraft(Item item, int amount);
}
