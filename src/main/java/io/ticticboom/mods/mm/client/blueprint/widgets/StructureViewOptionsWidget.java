package io.ticticboom.mods.mm.client.blueprint.widgets;

import com.google.common.collect.Lists;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiAlignment;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.util.GuiPlacementHelper;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.structure.StructureModel;
import org.joml.Vector3i;

import java.util.List;
import java.util.stream.IntStream;

public class StructureViewOptionsWidget extends AbstractWidget {
    private final GuiPlacementHelper guiHelper;
    private final StructureModel model;


    public StructureViewOptionsWidget(GuiPos pos, StructureModel model) {
        super(pos);
        guiHelper = new GuiPlacementHelper(pos, 5);
        this.model = model;
        addWidget(new ArrowOptionSelectWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(0,
                                0,
                                pos.w(),
                                ArrowOptionSelectWidget.DEFAULT_HEIGHT)), getYSliceOptions()));
    }

    private List<String> getYSliceOptions() {
        GuiStructureRenderer guiRenderer = model.getGuiRenderer();
        guiRenderer.init();
        Vector3i structureSize = guiRenderer.getStructureSize();
        return IntStream.rangeClosed(0, structureSize.y).boxed().map(String::valueOf).toList();
    }
}
