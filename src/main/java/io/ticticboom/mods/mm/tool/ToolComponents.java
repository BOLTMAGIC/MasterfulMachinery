package io.ticticboom.mods.mm.tool;

import com.mojang.serialization.Codec;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class ToolComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Ref.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.register("tool_energy",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> STORE = COMPONENTS.register("tool_store",
            () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> SETTINGS = COMPONENTS.register("tool_settings",
            () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());

    private ToolComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    public static CompoundTag read(ItemStack stack, DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> type) {
        var data = stack.get(type.get());
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void update(ItemStack stack, DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> type, Consumer<CompoundTag> change) {
        CustomData.update(type.get(), stack, change);
    }
}
