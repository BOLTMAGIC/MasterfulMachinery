package io.ticticboom.mods.mm.port.energy;

import java.util.List;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.UUID;

public class EnergyPortStorage implements IPortStorage {


    private final EnergyPortStorageModel model;
    @Getter
    private final EnergyPortHandler handler;
    private final UUID uid = UUID.randomUUID();

    // Priority for outputs. Default 0. Range 0..10.
    private int priority = 0;

    public EnergyPortStorage(EnergyPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        handler = new EnergyPortHandler(model.capacity(), model.maxReceive(), model.maxExtract(), changed);
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return MMCapabilities.ENERGY == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("handler", handler.serialize());
        tag.putInt("Priority", this.priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        handler.deserialize(tag.get("handler"));
        if (tag.contains("Priority")) {
            this.priority = Math.max(0, Math.min(10, tag.getIntOr("Priority", 0)));
        } else {
            this.priority = 0;
        }
    }

    @Override
    public IPortStorageModel getStorageModel() {
        return model;
    }

    @Override
    public IPortStorage simulationCopy(HolderLookup.Provider registries) {
        var copy = new EnergyPortStorage(model, () -> {});
        copy.load(save(new CompoundTag(), registries), registries);
        return copy;
    }

    @Override
    public UUID getStorageUid() {
        return uid;
    }

    @Override
    public JsonObject debugDump() {
        JsonObject dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("stored", handler.getStored());
        dump.addProperty("maxReceive", model.maxReceive());
        dump.addProperty("maxExtract", model.maxExtract());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("priority", this.priority);
        return dump;
    }

    public long internalExtract(long amount, boolean simulate) {
        return handler.extract(amount, simulate);
    }

    public long internalInsert(long amount, boolean simulate) {
        return handler.insert(amount, simulate);
    }

    public long getStoredEnergy() {
        return handler.getStored();
    }

    // Priority accessors
    public int getPriority() { return this.priority; }
    public void setPriority(int priority) { this.priority = Math.max(0, Math.min(10, priority)); }

    @Override
    public List<PortContent> contents() {
        return List.of(PortContent.energy(handler.getStored(), handler.getCapacity()));
    }
}
