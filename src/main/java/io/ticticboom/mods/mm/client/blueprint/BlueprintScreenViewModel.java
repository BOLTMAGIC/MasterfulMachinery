package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;

import java.util.List;

public class BlueprintScreenViewModel {
    @Getter
    private StructureModel structure;

   @Getter
    private final List<StructureModel> availableStructures = StructureManager.STRUCTURES.values().stream().toList();

    public BlueprintScreenViewModel() {
        this.structure = availableStructures.get(0);
    }

    public void setStructure(int index) {
        structure = availableStructures.get(index);
    }
}
