package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class MachineControllerScreen extends AbstractContainerScreen<MachineControllerMenu> {

    private final MachineControllerMenu menu;
    private final MachineControllerBlockEntity be;
    private final FormattedText header;

    public MachineControllerScreen(MachineControllerMenu menu, Inventory inv, Component p_96550_) {
        super(menu, inv, p_96550_);
        this.menu = menu;
        this.be = (MachineControllerBlockEntity) menu.getBe();
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().name();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // controller name
        gfx.drawWordWrap(this.font, header, 10, 10, 150, 0xacacac);

        // structure formation details
        var isFormed = be.getStructure() != null;
        gfx.drawWordWrap(this.font, FormattedText.of(isFormed ? "Formed As:" : "Not Formed"), 10, 40, 150, 0xacacac);
        if (isFormed) {
            gfx.drawWordWrap(this.font, FormattedText.of(be.getStructure().name()), 10, 53, 150, 0xacacac);
        }

        // show max parallel recipes under the multiblock name when formed
        if (isFormed) {
            int structVal = be.getStructure().maxParallelRecipes();
            int displayInt = 1;
            if (structVal > 0) {
                displayInt = structVal;
            } else {
                var controllerModel = io.ticticboom.mods.mm.setup.loader.ControllerLoader.CONTROLLER_MODELS.get(menu.getModel().id());
                if (controllerModel != null && controllerModel.maxParallelRecipes() > 0) {
                    displayInt = controllerModel.maxParallelRecipes();
                }
            }
            String line = "Max Parallel Processing: " + displayInt;
            int nameY = 53;
            int lineHeight = this.font.lineHeight;
            int subY = nameY + lineHeight + 10;
            // smaller text by scaling
            gfx.pose().pushPose();
            gfx.pose().translate(10f, (float) subY, 0f);
            float scale = 0.65f;
            gfx.pose().scale(scale, scale, 1f);
            int wrapWidth = (int) (150 / scale);
            TextRenderUtil.renderWordWrapLimit(gfx, line, 0, 0, wrapWidth, 1, 0xacacac);
            gfx.pose().popPose();
        }

        // recipe processing details
        var isProcessing = be.getRecipeState() != null;
        if (isProcessing) {
            gfx.drawWordWrap(this.font,
                    FormattedText
                            .of("Progress: " + String.format("%.2f", be.getRecipeState().getTickPercentage()) + "%"),
                    10, 110, 150, 0xacacac);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltip(gfx, mouseX, mouseY);
    }
}
