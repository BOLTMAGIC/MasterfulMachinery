package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.client.blueprint.widgets.StructureSelectWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiAlignment;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.util.GuiPosHelper;
import io.ticticboom.mods.mm.client.gui.widgets.TilingBackgroundGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void resize(@NotNull Minecraft pMinecraft, int pWidth, int pHeight) {
        super.resize(pMinecraft, pWidth, pHeight);
    }
}
