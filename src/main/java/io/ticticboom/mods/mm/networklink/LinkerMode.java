package io.ticticboom.mods.mm.networklink;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;

public enum LinkerMode {
    LINK,
    INFO;

    private static final String TAG = "Mode";

    public Component displayName() {
        return Component.translatable("mode.mm.network_linker." + name().toLowerCase(Locale.ROOT));
    }

    public LinkerMode cycle(int direction) {
        var values = values();
        return values[Math.floorMod(ordinal() + direction, values.length)];
    }

    public static LinkerMode get(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return LINK;
        }
        int ordinal = data.copyTag().getIntOr(TAG, 0);
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : LINK;
    }

    public void set(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG, ordinal()));
    }
}
