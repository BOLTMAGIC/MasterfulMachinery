package io.ticticboom.mods.mm.port;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IPortStorage {
    /**
     * NeoForge replaced LazyOptional with plain nullable returns, and Capability with
     * BlockCapability. Absent capability is now null rather than LazyOptional.empty().
     */
    <T> @Nullable T getCapability(BlockCapability<T, ?> capability);

    <T> boolean hasCapability(BlockCapability<T, ?> capability);

    // 1.21.1 serialises through a registry lookup, so the provider has to reach the storages.
    CompoundTag save(CompoundTag tag, HolderLookup.Provider registries);

    void load(CompoundTag tag, HolderLookup.Provider registries);

    IPortStorageModel getStorageModel();

    UUID getStorageUid();

    JsonObject debugDump();

    /**
     * A detached copy with the same contents and settings, which OutputSimulation fills to see whether several
     * outputs fit together; null when this kind of port can't be copied.
     */
    @Nullable
    default IPortStorage simulationCopy(HolderLookup.Provider registries) {
        return null;
    }

    default void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel model) {
        BlockUtils.setupPlayerInventory(container, inv, 0, 0);
    }

    // Priority for outputs. Server authoritative. Range expected to be clamped to [0,10].
    // Implementations should persist this value in their save/load methods under the key "Priority" if applicable.
    default int getPriority() {
        return 0;
    }

    default List<PortContent> contents() {
        return List.of();
    }

    default List<net.minecraft.network.chat.Component> describeContents() {
        return List.of();
    }

    default double fillRatio() {
        return -1;
    }

    default int fillColor() {
        return 0;
    }

    default void setPriority(int priority) {
        // default no-op for storage implementations that don't track priority
    }
}
