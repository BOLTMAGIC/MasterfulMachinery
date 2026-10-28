package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayEnergyHandler implements IEnergyStorage, TransferSnapshot {
    private final Supplier<List<IPortStorage>> inputs;
    private final Supplier<List<IPortStorage>> outputs;

    public GatewayEnergyHandler(Supplier<List<IPortStorage>> inputs) {
        this(inputs, List::of);
    }

    public GatewayEnergyHandler(Supplier<List<IPortStorage>> inputs, Supplier<List<IPortStorage>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private static List<IEnergyStorage> handlers(List<IPortStorage> storages) {
        var result = new ArrayList<IEnergyStorage>();
        for (IPortStorage storage : storages) {
            var handler = storage.getCapability(MMCapabilities.ENERGY);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    private List<IEnergyStorage> all() {
        var result = handlers(inputs.get());
        result.addAll(handlers(outputs.get()));
        return result;
    }

    @Override
    public Object createSnapshot() {
        return GatewaySnapshots.create(all());
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        GatewaySnapshots.restore(snapshot);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = 0;
        for (IEnergyStorage handler : handlers(inputs.get())) {
            if (received >= maxReceive) {
                break;
            }
            received += handler.receiveEnergy(maxReceive - received, simulate);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = 0;
        for (IEnergyStorage handler : handlers(outputs.get())) {
            if (extracted >= maxExtract) {
                break;
            }
            extracted += handler.extractEnergy(maxExtract - extracted, simulate);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        long stored = 0;
        for (IEnergyStorage handler : all()) {
            stored += handler.getEnergyStored();
        }
        return (int) Math.min(Integer.MAX_VALUE, stored);
    }

    @Override
    public int getMaxEnergyStored() {
        long capacity = 0;
        for (IEnergyStorage handler : all()) {
            capacity += handler.getMaxEnergyStored();
        }
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean canExtract() {
        return !handlers(outputs.get()).isEmpty();
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
