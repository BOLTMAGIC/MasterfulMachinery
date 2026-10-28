package io.ticticboom.mods.mm.debug.output.model;

import net.minecraft.resources.Identifier;

public class DebugPortBlockState {
    private final Identifier portId;
    private final Identifier portType;

    public DebugPortBlockState(Identifier portId, Identifier portType /*, state & contents data*/) {

        this.portId = portId;
        this.portType = portType;
    }
}
