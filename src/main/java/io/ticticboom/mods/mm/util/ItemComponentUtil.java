package io.ticticboom.mods.mm.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ItemComponentUtil {

    @Nullable
    private static HolderLookup.Provider registries;

    private ItemComponentUtil() {
    }

    public static void setRegistries(@Nullable HolderLookup.Provider provider) {
        registries = provider;
    }

    public static DataComponentPatch parse(JsonElement json) {
        DynamicOps<JsonElement> ops = registries == null ? JsonOps.INSTANCE : registries.createSerializationContext(JsonOps.INSTANCE);
        return DataComponentPatch.CODEC.parse(ops, json)
                .getOrThrow(error -> new RuntimeException("Invalid item components " + json + ": " + error));
    }

    public static boolean matches(ItemStack stack, DataComponentPatch required) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : required.entrySet()) {
            Object actual = stack.get(entry.getKey());
            if (entry.getValue().isPresent() ? !Objects.equals(actual, entry.getValue().get()) : actual != null) {
                return false;
            }
        }
        return true;
    }
}
