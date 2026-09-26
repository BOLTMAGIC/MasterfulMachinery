package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * FE storage for a Multiblock Tool stack, backed by an int tag ({@value #KEY}) on the tool's own NBT.
 */
public class ToolEnergy implements IEnergyStorage {
    private static final String KEY = "Energy";

    private final ItemStack stack;
    private final int capacity;

    public ToolEnergy(ItemStack stack, int capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int rate = MMConfigSetup.COMMON.toolEnergyReceiveRate.get();
        int received = Math.min(Math.min(maxReceive, rate), capacity - getEnergyStored());
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

    @Override
    public int getEnergyStored() {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        return Math.max(0, Math.min(capacity, tag.getInt(KEY)));
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
        stack.getOrCreateTag().putInt(KEY, Math.max(0, Math.min(capacity, value)));
    }
}
