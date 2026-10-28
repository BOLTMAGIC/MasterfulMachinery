package io.ticticboom.mods.mm.port.ars.source.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortStorageModel;

public class ArsSourceConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity;
    private int range = 6;

    public ArsSourceConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public ArsSourceConfigBuilderJS range(int range) {
        this.range = range;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new ArsSourcePortStorageModel(capacity, range);
    }
}
