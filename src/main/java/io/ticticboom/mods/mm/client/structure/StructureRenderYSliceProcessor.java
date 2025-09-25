package io.ticticboom.mods.mm.client.structure;

import lombok.Getter;
import lombok.Setter;

public class StructureRenderYSliceProcessor {
    @Getter
    @Setter
    private int ySlice = 0;

    @Getter
    @Setter
    private boolean shouldSlice = false;

    public boolean canProcess(PositionedCyclingBlockRenderer part) {
        if (!shouldSlice) {
            return true;
        }
        return part.pos.getY() == ySlice;
    }
}
