package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.util.GuiAlignment;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.util.GuiPlacementHelper;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.structure.StructureModel;

public class StructureViewOptionsWidget extends AbstractWidget {
    private final GuiPlacementHelper guiHelper;
    private final StructureModel model;
    private final BlueprintStructureViewState viewModel = new BlueprintStructureViewState();

    public final GuiEventHandler<BlueprintStructureViewState> changeEmitter = new GuiEventHandler<>();

    public StructureViewOptionsWidget(GuiPos pos, StructureModel model) {
        super(pos);
        guiHelper = new GuiPlacementHelper(pos, 5);
        this.model = model;
        createYSlicer();
//        createZoom();
    }
    
    private void createYSlicer() {
        var ySlicer = addWidget(new BlueprintViewYSliceWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(0,
                                0,
                                guiHelper.getGuiWidth(),
                                ArrowOptionSelectWidget.DEFAULT_HEIGHT)), model));

        ySlicer.changeEmitter.addListener(e -> {
            viewModel.setSlice(e.ySlice(), e.shouldSlice());
            changeEmitter.fireEvent(viewModel);
        });
    }

    private void createZoom() {
        var verticalSpacing = ArrowOptionSelectWidget.DEFAULT_HEIGHT + 4;
        var zoom = addWidget(new BlueprintViewZoomWidget(
                guiHelper.offset(GuiAlignment.LEFT_TOP,
                        GuiPos.of(0, verticalSpacing, guiHelper.getGuiWidth(), ArrowOptionSelectWidget.DEFAULT_HEIGHT))));

        zoom.changeEmitter.addListener(e -> {
            viewModel.setZoom(e.zoom());
            changeEmitter.fireEvent(viewModel);
        });
    }
}
