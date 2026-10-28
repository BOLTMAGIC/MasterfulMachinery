package io.ticticboom.mods.mm.port.mekanism.heat.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorageModel;
import mekanism.api.heat.HeatAPI;

public class MekanismHeatConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity;
    private double heatCapacity = MekanismHeatPortStorageModel.DEFAULT_HEAT_CAPACITY;
    private double inverseConduction = HeatAPI.DEFAULT_INVERSE_CONDUCTION;
    private boolean autoPush = true;

    public MekanismHeatConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public MekanismHeatConfigBuilderJS heatCapacity(double heatCapacity) {
        this.heatCapacity = heatCapacity;
        return this;
    }

    public MekanismHeatConfigBuilderJS inverseConduction(double inverseConduction) {
        this.inverseConduction = inverseConduction;
        return this;
    }

    public MekanismHeatConfigBuilderJS autoPush(boolean autoPush) {
        this.autoPush = autoPush;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new MekanismHeatPortStorageModel(
                capacity,
                heatCapacity <= 0 ? MekanismHeatPortStorageModel.DEFAULT_HEAT_CAPACITY : heatCapacity,
                inverseConduction <= 0 ? HeatAPI.DEFAULT_INVERSE_CONDUCTION : inverseConduction,
                () -> autoPush,
                getTierRank());
    }
}
