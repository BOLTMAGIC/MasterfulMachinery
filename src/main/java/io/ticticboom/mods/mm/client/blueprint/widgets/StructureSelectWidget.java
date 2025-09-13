package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.structure.StructureModel;

import java.util.List;

public class StructureSelectWidget extends ArrowOptionSelectWidget {
    public StructureSelectWidget(GuiPos pos, List<StructureModel> structures) {
        super(pos, structures.stream().map(StructureModel::name).toList());
    }
}
