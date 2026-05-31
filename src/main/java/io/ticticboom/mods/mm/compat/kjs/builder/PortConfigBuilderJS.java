package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;

public abstract class PortConfigBuilderJS {
    @HideFromJS
    protected int tierRank = 0;

    public PortConfigBuilderJS tierRank(int tierRank) {
        this.tierRank = tierRank;
        return this;
    }

    @HideFromJS
    public int getTierRank() {
        return tierRank;
    }

    @HideFromJS
    public abstract IPortStorageModel build();
}
