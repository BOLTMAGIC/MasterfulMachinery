package io.ticticboom.mods.mm.port;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;
import java.util.UUID;

public interface IPortStorage {
    <T> LazyOptional<T> getCapability(Capability<T> capability);

    <T> boolean hasCapability(Capability<T> capability);

    CompoundTag save(CompoundTag tag);

    void load(CompoundTag tag);

    IPortStorageModel getStorageModel();

    UUID getStorageUid();

    JsonObject debugDump();

    /**
     * A detached copy with the same contents and settings, which OutputSimulation fills to see whether several
     * outputs fit together; null when this kind of port can't be copied.
     */
    @org.jetbrains.annotations.Nullable
    default IPortStorage simulationCopy() {
        return null;
    }

    /**
     * Whether an AE2 pattern carries what this kind of port holds (items, fluids, Mekanism chemicals): a pattern run
     * takes and puts those in its own buffer, and uses the machine's ports only for everything else.
     */
    default boolean carriedByPatterns() {
        return false;
    }

    /**
     * @return what the port holds, for the controller's port list; empty when there is nothing to show
     */
    default List<PortContent> contents() {
        return List.of();
    }

    default void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel model) {
        BlockUtils.setupPlayerInventory(container, inv, 0, 0);
    }

    // Priority for outputs. Server authoritative. Range expected to be clamped to [0,10].
    // Implementations should persist this value in their save/load methods under the key "Priority" if applicable.
    default int getPriority() {
        return 0;
    }

    default void setPriority(int priority) {
        // default no-op for storage implementations that don't track priority
    }
}
