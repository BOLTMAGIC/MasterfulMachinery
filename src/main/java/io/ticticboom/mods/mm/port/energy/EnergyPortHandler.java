package io.ticticboom.mods.mm.port.energy;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.nbt.Tag;
import net.minecraftforge.energy.EnergyStorage;

public class EnergyPortHandler extends EnergyStorage {
    private final INotifyChangeFunction changed;

    public EnergyPortHandler(int capacity, int maxReceive, int maxExtract, INotifyChangeFunction changed) {
        super(capacity, maxReceive, maxExtract);
        this.changed = changed;
    }

    public long unboundedReceiveEnergy(long maxReceive, boolean simulate) {
        // Handle large amounts by receiving in chunks to avoid int overflow
        if (maxReceive <= Integer.MAX_VALUE) {
            return unboundedReceiveEnergyInt((int) maxReceive, simulate);
        }

        long received = 0;
        long remaining = maxReceive;
        int simulatedEnergy = this.energy;  // Track energy state during all operations

        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            int canReceive = Math.max(0, Math.min(this.capacity - simulatedEnergy, chunk));
            
            // Always update simulated state consistently
            simulatedEnergy += canReceive;
            received += canReceive;

            if (canReceive < chunk) break; // Can't receive more
            remaining -= canReceive;
        }

        // Apply final state to real energy if not simulating
        if (!simulate) {
            this.energy = simulatedEnergy;
            changed.call();
        }
        return received;
    }

    private int unboundedReceiveEnergyInt(int maxReceive, boolean simulate) {
        // room left, not energy + maxReceive: that sum overflows int near an Integer.MAX_VALUE capacity
        int canReceive = Math.max(0, Math.min(this.capacity - this.energy, maxReceive));
        if (!simulate) {
            this.energy += canReceive;
            changed.call();
        }
        return canReceive;
    }

    public long unboundedExtractEnergy(long maxExtract, boolean simulate) {
        // Handle large amounts by extracting in chunks to avoid int overflow
        if (maxExtract <= Integer.MAX_VALUE) {
            return unboundedExtractEnergyInt((int) maxExtract, simulate);
        }

        long extracted = 0;
        long remaining = maxExtract;
        int simulatedEnergy = this.energy;  // Track energy state during all operations

        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            int canExtract = Math.min(simulatedEnergy, chunk);
            
            // Always update simulated state consistently
            simulatedEnergy -= canExtract;
            extracted += canExtract;

            if (canExtract < chunk) break; // Can't extract more
            remaining -= canExtract;
        }

        // Apply final state to real energy if not simulating
        if (!simulate) {
            this.energy = simulatedEnergy;
            changed.call();
        }
        return extracted;
    }

    private int unboundedExtractEnergyInt(int maxExtract, boolean simulate) {
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
