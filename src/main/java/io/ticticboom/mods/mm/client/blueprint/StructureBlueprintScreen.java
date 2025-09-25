package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.client.MMAbstractScreen;
import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
import io.ticticboom.mods.mm.client.blueprint.widgets.StructureRenderWidget;
import io.ticticboom.mods.mm.client.blueprint.widgets.StructureSelectWidget;
import io.ticticboom.mods.mm.client.blueprint.widgets.StructureViewOptionsWidget;
import io.ticticboom.mods.mm.client.gui.IWidget;
import io.ticticboom.mods.mm.client.gui.util.*;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.client.gui.widgets.TilingBackgroundGui;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class StructureBlueprintScreen extends MMAbstractScreen {

    private final BlueprintScreenViewModel viewModel = new BlueprintScreenViewModel();
    private final ArrayList<IWidget> structureWidgets = new ArrayList<>();
    private GuiPlacementHelper guiHelper;
    private BlueprintStructureViewState viewState = new BlueprintStructureViewState();

    @Override
    protected void init() {
        guiHelper = new GuiPlacementHelper(this.width, this.height, 10);
        addRenderableOnly(new TilingBackgroundGui(guiHelper.getGuiPos()));

        var structureSelector = addRenderableWidget(new StructureSelectWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(5, 5, guiHelper.getGuiWidth() - 10, 20)), viewModel.getAvailableStructures()));

        setupStructureDependentWidgets(viewModel.getStructure());

        structureSelector.changeEmitter
                .addListener(e ->
                        schedule(() -> updateStructure(e.index())));
    }

    private void updateStructure(int index) {
        viewModel.setStructure(index);
        resetStructureDependantWidgets();
        var model = viewModel.getStructure();
        if (model != null) {
            setupStructureDependentWidgets(model);
        }
    }

    private void resetStructureDependantWidgets() {
        structureWidgets.forEach(this::removeWidget);
        structureWidgets.clear();
    }

    private void setupStructureDependentWidgets(StructureModel model) {
        int column = guiHelper.columnWidth(3);
        int fromTop = 25;
        viewState = new BlueprintStructureViewState();
        addStructureDependantWidget(new StructureRenderWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(5,
                                fromTop,
                                column,
                                guiHelper.fromBottom(5, fromTop))),
                viewModel.getStructure(),
                () -> viewState));

        var viewSelect = addStructureDependantWidget(new StructureViewOptionsWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(column + 5,
                                fromTop,
                                column,
                                guiHelper.fromBottom(5, fromTop))), model));

        viewSelect.changeEmitter.addListener(e -> viewState = e);
    }

    private <T extends IWidget> T addStructureDependantWidget(T widget) {
        structureWidgets.add(widget);
        addRenderableWidget(widget);
        return widget;
    }
}
