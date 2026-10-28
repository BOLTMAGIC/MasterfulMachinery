package io.ticticboom.mods.mm.port.mekanism.heat;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MekanismHeatPortStorage implements IPortStorage {

    private final MekanismHeatPortStorageModel model;
    @Getter
    private final MekanismHeatPortHandler handler;
    private final UUID uid = UUID.randomUUID();

    private int priority = 0;

    public MekanismHeatPortStorage(MekanismHeatPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.handler = new MekanismHeatPortHandler(model, changed);
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return MekCapabilities.HEAT == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("Heat", handler.getStored());
        tag.putInt("Priority", this.priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        handler.setStored(tag.getDoubleOr("Heat", 0));
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
    public UUID getStorageUid() {
        return uid;
    }

    @Override
    public JsonObject debugDump() {
        JsonObject dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("stored", handler.getStored());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("heatCapacity", model.heatCapacity());
        dump.addProperty("inverseConduction", model.inverseConduction());
        dump.addProperty("temperature", handler.getTemperature(0));
        dump.addProperty("priority", this.priority);
        return dump;
    }

    public int internalExtract(int amount, boolean simulate) {
        return handler.extract(amount, simulate);
    }

    public int internalInsert(int amount, boolean simulate) {
        return handler.insert(amount, simulate);
    }

    @Override
    public java.util.List<PortContent> contents() {
        var capacity = ((MekanismHeatPortStorageModel) getStorageModel()).capacity();
        return java.util.List.of(PortContent.gauge(net.minecraft.network.chat.Component.translatable("port.mm.mekanism_heat.name"), 0xE67E22, getStoredHeat(), capacity, "J"));
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> describeContents() {
        var capacity = ((MekanismHeatPortStorageModel) getStorageModel()).capacity();
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable("gui.mm.port.mekanism_heat.temperature",
                        String.format(java.util.Locale.ROOT, "%.1f", getTemperature())),
                net.minecraft.network.chat.Component.translatable("gui.mm.port.mekanism_heat.storage",
                        getStoredHeat(), capacity));
    }

    public int getStoredHeat() {
        return (int) Math.floor(handler.getStored());
    }

    public double getTemperature() {
        return handler.getTemperature(0);
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(10, priority));
    }
}
