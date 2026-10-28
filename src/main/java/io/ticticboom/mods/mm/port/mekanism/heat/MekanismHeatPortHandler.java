package io.ticticboom.mods.mm.port.mekanism.heat;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;

public class MekanismHeatPortHandler implements IHeatHandler {

    private final MekanismHeatPortStorageModel model;
    private final INotifyChangeFunction changed;
    private double stored;

    public MekanismHeatPortHandler(MekanismHeatPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
    }

    public double getStored() {
        return stored;
    }

    public void setStored(double value) {
        stored = Math.max(0, Math.min(model.capacity(), value));
    }

    @Override
    public int getHeatCapacitorCount() {
        return 1;
    }

    @Override
    public double getTemperature(int capacitor) {
        return HeatAPI.AMBIENT_TEMP + stored / model.heatCapacity();
    }

    @Override
    public double getInverseConduction(int capacitor) {
        return model.inverseConduction();
    }

    @Override
    public double getHeatCapacity(int capacitor) {
        return model.heatCapacity();
    }

    @Override
    public void handleHeat(int capacitor, double amount) {
        move(amount);
    }

    public double move(double amount) {
        double before = stored;
        setStored(stored + amount);
        double moved = stored - before;
        if (Math.abs(moved) > HeatAPI.EPSILON) {
            changed.call();
        }
        return moved;
    }

    public int insert(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = (int) Math.min(amount, Math.floor(model.capacity() - stored));
        if (accepted > 0 && !simulate) {
            move(accepted);
        }
        return Math.max(0, accepted);
    }

    public int extract(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int removed = (int) Math.min(amount, Math.floor(stored));
        if (removed > 0 && !simulate) {
            move(-removed);
        }
        return Math.max(0, removed);
    }

    public void exchangeWith(IHeatHandler adjacent, boolean pushing) {
        double resistance = getInverseConduction(0) + adjacent.getTotalInverseConduction();
        if (resistance <= 0) {
            return;
        }
        double difference = pushing
                ? getTemperature(0) - adjacent.getTotalTemperature()
                : adjacent.getTotalTemperature() - getTemperature(0);
        if (difference <= 0) {
            return;
        }
        double moved = (difference / resistance) * getHeatCapacity(0);
        if (moved <= HeatAPI.EPSILON) {
            return;
        }
        if (pushing) {
            double taken = -move(-moved);
            if (taken > 0) {
                adjacent.handleHeat(taken);
            }
        } else {
            double given = move(moved);
            if (given > 0) {
                adjacent.handleHeat(-given);
            }
        }
    }
}
