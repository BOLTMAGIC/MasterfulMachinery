package io.ticticboom.mods.mm.port.energy;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyPortHandler implements IEnergyStorage, TransferSnapshot {
    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private final INotifyChangeFunction changed;
    private long energy;

    public EnergyPortHandler(long capacity, long maxReceive, long maxExtract, INotifyChangeFunction changed) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.changed = changed;
    }

    @Override
    public Object createSnapshot() {
        return energy;
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        energy = (Long) snapshot;
        changed.call();
    }

    public long getStored() {
        return energy;
    }

    public long getCapacity() {
        return capacity;
    }

    public long insert(long amount, boolean simulate) {
        long accepted = Math.max(0, Math.min(amount, capacity - energy));
        if (!simulate && accepted > 0) {
            energy += accepted;
            changed.call();
        }
        return accepted;
    }

    public long extract(long amount, boolean simulate) {
        long taken = Math.max(0, Math.min(amount, energy));
        if (!simulate && taken > 0) {
            energy -= taken;
            changed.call();
        }
        return taken;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }
        return (int) insert(Math.min(toReceive, maxReceive), simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!canExtract() || toExtract <= 0) {
            return 0;
        }
        return (int) extract(Math.min(toExtract, maxExtract), simulate);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(energy, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    public Tag serialize() {
        return LongTag.valueOf(energy);
    }

    public void deserialize(Tag tag) {
        energy = tag instanceof NumericTag number ? Math.max(0, Math.min(capacity, number.longValue())) : 0;
    }
}
