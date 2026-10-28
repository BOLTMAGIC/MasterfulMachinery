package io.ticticboom.mods.mm.port.projecte.emc.register;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortStorage;
import io.ticticboom.mods.mm.util.NumberText;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class ProjectEEmcPortScreen extends AbstractContainerScreen<ProjectEEmcPortMenu> {

    private final FormattedText header;

    public ProjectEEmcPortScreen(ProjectEEmcPortMenu menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(gfx, mouseX, mouseY, partialTicks);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        ProjectEEmcPortBlockEntity be = menu.getBlockEntity();
        if (be.getEmcStorage().hasKleinSlot()) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, this.leftPos + ProjectEEmcPortStorage.KLEIN_SLOT_X, this.topPos + ProjectEEmcPortStorage.KLEIN_SLOT_Y, 0, 26, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        gfx.textWithWordWrap(this.font, header, 8, 8, 150, TextColors.argb(0x404040));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, this.leftPos + 7, this.topPos + 50, 89, 78, 162, 80, 256, 256);
        ProjectEEmcPortBlockEntity be = menu.getBlockEntity();
        ProjectEEmcPortStorage storage = be.getEmcStorage();
        var filledValue = (double) storage.getStored() / (double) storage.getCapacity();
        var filledHeight = (int) (Math.min(filledValue, 1) * 78);
        var start = 129 - filledHeight;
        gfx.fill(this.leftPos + 8, this.topPos + start, this.leftPos + 168, this.topPos + start + filledHeight, ProjectEEmcPortStorage.FILL_COLOR);
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + 7, this.topPos + 50, 162, 80)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.translatable("gui.mm.port.projecte_emc.storage",
                    NumberText.grouped(storage.getStored()), NumberText.grouped(storage.getCapacity())));
            gfx.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }
}
