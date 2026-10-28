package io.ticticboom.mods.mm.port.ars.source;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import io.ticticboom.mods.mm.cap.ArsCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ArsSourcePortStorage implements IPortStorage {

    public static final int FILL_COLOR = 0xFF883090;

    private final ArsSourcePortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();

    @Getter
    private final int capacity;
    @Getter
    private int stored = 0;
    @Setter
    private boolean input = false;
    private final SourceCap sourceCap = new SourceCap();

    public ArsSourcePortStorage(ArsSourcePortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.capacity = model.capacity();
    }

    public int getStored() {
        return stored;
    }

    public int getRange() {
        return model.range();
    }

    public int receive(int source, boolean simulate) {
        var accepted = Math.min(capacity - stored, Math.abs(source));
        if (!simulate && accepted > 0) {
            stored += accepted;
            changed.call();
        }
        return accepted;
    }

    public int extract(int source, boolean simulate) {
        var drained = Math.min(stored, Math.abs(source));
        if (!simulate && drained > 0) {
            stored -= drained;
            changed.call();
        }
        return drained;
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) sourceCap;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == ArsCapabilities.SOURCE;
    }

    private class SourceCap implements ISourceCap {

        @Override
        public boolean canAcceptSource(int amount) {
            return input && capacity - stored >= amount;
        }

        @Override
        public boolean canProvideSource(int amount) {
            return !input && stored >= amount;
        }

        @Override
        public int getMaxExtract() {
            return input ? 0 : capacity;
        }

        @Override
        public int getMaxReceive() {
            return input ? capacity : 0;
        }

        @Override
        public int getSource() {
            return stored;
        }

        @Override
        public int getSourceCapacity() {
            return capacity;
        }

        @Override
        public void setSource(int source) {
            stored = Math.max(0, Math.min(capacity, source));
            changed.call();
        }

        @Override
        public void setMaxSource(int max) {
        }

        @Override
        public int receiveSource(int amount, boolean simulate) {
            return input ? receive(amount, simulate) : 0;
        }

        @Override
        public int extractSource(int amount, boolean simulate) {
            return input ? 0 : extract(amount, simulate);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("stored", stored);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored = tag.getIntOr("stored", 0);
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
    public List<PortContent> contents() {
        return List.of(PortContent.gauge(Component.translatable("port.mm.ars_source.name"), FILL_COLOR, stored, capacity, "Source"));
    }

    @Override
    public List<Component> describeContents() {
        return List.of(Component.translatable("gui.mm.port.ars_source.storage", stored, capacity));
    }

    @Override
    public double fillRatio() {
        return capacity <= 0 ? -1 : Math.min(1, (double) stored / capacity);
    }

    @Override
    public int fillColor() {
        return FILL_COLOR;
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("stored", stored);
        json.addProperty("capacity", capacity);
        json.addProperty("range", model.range());
        return json;
    }
}
