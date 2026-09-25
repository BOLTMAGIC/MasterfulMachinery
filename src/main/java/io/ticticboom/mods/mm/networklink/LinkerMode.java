package io.ticticboom.mods.mm.networklink;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public enum LinkerMode {
    /** Save a network, link / unlink controllers. */
    LINK,
    /** Show what the clicked machine is linked to; sneak + right-click removes the link. */
    INFO;

    private static final String TAG = "Mode";

    public Component displayName() {
        return Component.translatable("mode.mm.network_linker." + name().toLowerCase());
    }

    public LinkerMode cycle(int direction) {
        var values = values();
        return values[Math.floorMod(ordinal() + direction, values.length)];
    }

    public static LinkerMode get(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null) {
            return LINK;
        }
        int ordinal = tag.getInt(TAG);
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : LINK;
    }

    public void set(ItemStack stack) {
        stack.getOrCreateTag().putInt(TAG, ordinal());
    }
}
