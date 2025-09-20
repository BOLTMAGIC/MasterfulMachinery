package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.client.blueprint.widgets.StructureRenderWidget;
import io.ticticboom.mods.mm.client.blueprint.widgets.StructureSelectWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiAlignment;
import io.ticticboom.mods.mm.client.gui.util.GuiCoord;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.util.GuiPosHelper;
import io.ticticboom.mods.mm.client.gui.widgets.TilingBackgroundGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StructureBlueprintScreen extends Screen {

    private final BlueprintScreenViewModel viewModel = new BlueprintScreenViewModel();

    protected StructureBlueprintScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        var guiHelper = new GuiPosHelper(this.width, this.height, 10);
        addRenderableOnly(new TilingBackgroundGui(guiHelper.getGuiPos()));

        var structureSelector = addRenderableWidget(new StructureSelectWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(5, 5, guiHelper.getGuiWidth() - 10, 20)), viewModel.getAvailableStructures()));

        structureSelector.changeEmitter
                .addListener(e -> viewModel.setStructure(e.index()));

        addRenderableWidget(new StructureRenderWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(5, 25, 250,
                                guiHelper.fromBottom(5, 25))), viewModel::getStructure));
    }
}
