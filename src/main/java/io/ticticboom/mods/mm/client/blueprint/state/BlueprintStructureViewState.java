package io.ticticboom.mods.mm.client.blueprint.state;

import lombok.Getter;

@Getter
public class BlueprintStructureViewState {
    private int ySlice = 0;
    private boolean shouldSlice = false;

    public void setSlice(int y, boolean shouldSlice) {
        this.ySlice = y;
        this.shouldSlice = shouldSlice;
    }
}
