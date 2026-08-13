package io.ticticboom.mods.mm.port.energy;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;

public class EnergyPortHandler extends EnergyStorage {
    private final INotifyChangeFunction changed;

    public EnergyPortHandler(int capacity, int maxReceive, int maxExtract, INotifyChangeFunction changed) {
        super(capacity, maxReceive, maxExtract);
        this.changed = changed;
    }

    public int unboundedReceiveEnergy(int maxReceive, boolean simulate) {
        int canReceive = Math.min(this.energy + maxReceive, this.capacity) - this.energy;
        canReceive = Math.min(canReceive, maxReceive);
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
