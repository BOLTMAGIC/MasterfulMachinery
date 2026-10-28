package io.ticticboom.mods.mm.port.fluid.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FluidPortConfigBuilderJS extends PortConfigBuilderJS {

    private int rows;
    private int columns;
    private int slotCapacity;
    private boolean isAutoPushSet = false;
    private boolean autoPush = false;
    private final List<String> fluids = new ArrayList<>();


    @Override
    public IPortStorageModel build() {
        return new FluidPortStorageModel(rows, columns, slotCapacity, isAutoPushSet ? () -> autoPush : () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, getTierRank(), List.copyOf(fluids));
    }

    public FluidPortConfigBuilderJS rows(int rows) {
        this.rows = rows;
        return this;
    }

    public FluidPortConfigBuilderJS columns(int columns) {
        this.columns = columns;
        return this;
    }

    public FluidPortConfigBuilderJS slotCapacity(int slotCapacity) {
        this.slotCapacity = slotCapacity;
        return this;
    }

    public FluidPortConfigBuilderJS fluids(String... fluids) {
        this.fluids.addAll(List.of(fluids));
        return this;
    }

    public FluidPortConfigBuilderJS autoPush(boolean autoPush) {
        this.autoPush = autoPush;
        this.isAutoPushSet = true;
        return this;
    }
}
