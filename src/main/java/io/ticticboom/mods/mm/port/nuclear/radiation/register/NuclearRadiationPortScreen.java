package io.ticticboom.mods.mm.port.nuclear.radiation.register;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

public class NuclearRadiationPortScreen extends AbstractContainerScreen<NuclearRadiationPortMenu> {

    private static final int MAX_NAME = 55;

    private final FormattedText header;
    private final NuclearRadiationPortBlockEntity be;

    public NuclearRadiationPortScreen(NuclearRadiationPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        header = FormattedText.of(name.length() > MAX_NAME ? name.substring(0, MAX_NAME) + "..." : name);
        be = menu.getBlockEntity();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (be.isInput()) {
            slot(gfx, NuclearRadiationPortStorage.INPUT_SLOT_X);
        } else {
            slot(gfx, NuclearRadiationPortStorage.CARRIER_SLOT_X);
            slot(gfx, NuclearRadiationPortStorage.LOADED_SLOT_X);
        }
    }

    private void slot(GuiGraphicsExtractor gfx, int x) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, this.leftPos + x, this.topPos + NuclearRadiationPortStorage.SLOT_Y, 0, 26, 18, 18, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        gfx.textWithWordWrap(this.font, header, 8, 8, 158, TextColors.argb(0x404040));
        int y = 24;
        for (Component line : be.getRadiationStorage().guiLines()) {
            gfx.text(this.font, line, 8, y, TextColors.argb(0x404040), false);
            y += 11;
        }
        if (!be.isInput()) {
            gfx.text(this.font, "→", NuclearRadiationPortStorage.CARRIER_SLOT_X + 22, NuclearRadiationPortStorage.SLOT_Y + 5, TextColors.argb(0x404040), false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
    }
}
