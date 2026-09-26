package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Passes received energy on to the machine's energy input ports, filling them in order.
 * Reports their combined stored energy and capacity; nothing can be extracted.
 */
public class GatewayEnergyHandler implements IEnergyStorage {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayEnergyHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IEnergyStorage> handlers() {
        var result = new ArrayList<IEnergyStorage>();
        for (IPortStorage storage : inputs.get()) {
            storage.getCapability(MMCapabilities.ENERGY).resolve().ifPresent(result::add);
        }
        return result;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = 0;
        for (IEnergyStorage handler : handlers()) {
            if (received >= maxReceive) {
                break;
            }
            received += handler.receiveEnergy(maxReceive - received, simulate);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        long stored = 0;
        for (IEnergyStorage handler : handlers()) {
            stored += handler.getEnergyStored();
        }
        return (int) Math.min(Integer.MAX_VALUE, stored);
    }

    @Override
    public int getMaxEnergyStored() {
        long capacity = 0;
        for (IEnergyStorage handler : handlers()) {
            capacity += handler.getMaxEnergyStored();
        }
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
