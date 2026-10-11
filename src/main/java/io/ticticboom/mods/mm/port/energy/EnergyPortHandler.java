package io.ticticboom.mods.mm.port.energy;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.energy.EnergyStorage;

public class EnergyPortHandler extends EnergyStorage {
    private final INotifyChangeFunction changed;

    public EnergyPortHandler(int capacity, int maxReceive, int maxExtract, INotifyChangeFunction changed) {
        super(capacity, maxReceive, maxExtract);
        this.changed = changed;
    }

    public int unboundedReceiveEnergy(int maxReceive, boolean simulate) {
        // room left, not energy + maxReceive: that sum overflows int near an Integer.MAX_VALUE capacity
        int canReceive = Math.max(0, Math.min(this.capacity - this.energy, maxReceive));
        if (!simulate) {
            this.energy += canReceive;
            changed.call();
        }
        return canReceive;
    }

    public int unboundedExtractEnergy(int maxExtract, boolean simulate) {
        int canExtract = Math.min(this.energy, maxExtract);
        if (!simulate) {
            this.energy -= canExtract;
            changed.call();
        }
        return canExtract;
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        super.deserializeNBT(nbt);
        // ports saved while the overflow above was hit hold a negative amount
        this.energy = Math.max(0, Math.min(this.energy, this.capacity));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int result = super.receiveEnergy(maxReceive, simulate);
        if (result > 0 && !simulate) {
            changed.call();
        }
        return result;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int result = super.extractEnergy(maxExtract, simulate);
        if (result > 0 && !simulate) {
            changed.call();
        }
        return result;
    }

}
