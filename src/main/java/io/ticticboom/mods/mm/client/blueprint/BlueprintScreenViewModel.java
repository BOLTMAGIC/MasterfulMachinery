package io.ticticboom.mods.mm.client.blueprint;

import com.google.common.collect.Lists;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import org.joml.Vector3i;

import java.util.Collections;
import java.util.List;

public class BlueprintScreenViewModel {
    @Getter
    private StructureModel structure;

   @Getter
    private final List<StructureModel> availableStructures = StructureManager.STRUCTURES.values().stream().toList();

    public BlueprintScreenViewModel() {
        this.structure = availableStructures.get(0);
        this.structure.getGuiRenderer().resetTransforms();
    }

    public void setStructure(int index) {
        structure = availableStructures.get(index);
        structure.getGuiRenderer().resetTransforms();
    }


}
