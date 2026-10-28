package io.ticticboom.mods.mm.port.projecte.emc.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortStorageModel;

public class ProjectEEmcConfigBuilderJS extends PortConfigBuilderJS {

    private long capacity;
    private boolean kleinSlot = false;
    private long kleinRate = 0;

    public ProjectEEmcConfigBuilderJS capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public ProjectEEmcConfigBuilderJS kleinSlot(boolean kleinSlot) {
        this.kleinSlot = kleinSlot;
        return this;
    }

    public ProjectEEmcConfigBuilderJS kleinRate(long kleinRate) {
        this.kleinRate = kleinRate;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new ProjectEEmcPortStorageModel(capacity, kleinSlot, kleinRate);
    }
}
