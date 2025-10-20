package io.ticticboom.mods.mm.client.blueprint.state;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BlueprintStructureViewState {
    private int ySlice = 0;
    private boolean shouldSlice = false;
    @Setter
    private int zoom = 10;

    public void setSlice(int y, boolean shouldSlice) {
        this.ySlice = y;
        this.shouldSlice = shouldSlice;
    }

}
