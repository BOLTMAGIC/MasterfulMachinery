package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class ToolEnergy implements IEnergyStorage {

    private final ItemStack stack;
    private final int capacity;

    public ToolEnergy(ItemStack stack, int capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    public static ToolEnergy of(ItemStack stack) {
        return new ToolEnergy(stack, MMConfig.TOOL_ENERGY_CAPACITY);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(Math.min(maxReceive, MMConfig.TOOL_ENERGY_RECEIVE_RATE), capacity - getEnergyStored());
        if (received <= 0) {
            return 0;
        }
        if (!simulate) {
            setEnergy(getEnergyStored() + received);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.min(maxExtract, getEnergyStored());
        if (extracted <= 0) {
            return 0;
        }
        if (!simulate) {
            setEnergy(getEnergyStored() - extracted);
        }
        return extracted;
    }

    public void restore(int fe) {
        if (fe > 0) {
            setEnergy(getEnergyStored() + fe);
        }
    }

    @Override
    public int getEnergyStored() {
        return Math.max(0, Math.min(capacity, stack.getOrDefault(ToolComponents.ENERGY.get(), 0)));
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    private void setEnergy(int value) {
        stack.set(ToolComponents.ENERGY.get(), Math.max(0, Math.min(capacity, value)));
    }
}
