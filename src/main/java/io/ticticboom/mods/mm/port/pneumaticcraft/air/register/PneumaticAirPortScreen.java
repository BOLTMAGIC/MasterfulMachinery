package io.ticticboom.mods.mm.port.pneumaticcraft.air.register;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorageModel;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class PneumaticAirPortScreen  extends AbstractContainerScreen<PneumaticAirPortMenu> {

    private final FormattedText header;

    public PneumaticAirPortScreen(PneumaticAirPortMenu menu, Inventory inv, Component displayName) {
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int p_282681_, int p_283686_) {
        gfx.textWithWordWrap(this.font, header, 8, 8, 150, TextColors.argb(0x404040));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
        PneumaticAirPortBlockEntity be = menu.getBE();
        PneumaticAirPortStorage storage = (PneumaticAirPortStorage) be.getStorage();
        PneumaticAirPortStorageModel model = be.getStorageModel();
        float air = storage.getAir();
        float pressure = storage.getPressure();
        float volume = storage.getVolume();
        var filledValue = (double)pressure / (double)storage.getAirhandler().getDangerPressure();
        var filledHeight = (int)(filledValue * 78);
        var start = 129 - filledHeight;
        gfx.textWithWordWrap(this.font, Component.translatable("gui.mm.port.pneumaticcraft_air.air", air), this.leftPos + 8, this.topPos + 30, 150, TextColors.argb(0x404040));
        gfx.textWithWordWrap(this.font, Component.translatable("gui.mm.port.pneumaticcraft_air.pressure", pressure), this.leftPos + 8, this.topPos + 40, 150, TextColors.argb(0x404040));
        gfx.textWithWordWrap(this.font, Component.translatable("gui.mm.port.pneumaticcraft_air.volume", volume), this.leftPos + 8, this.topPos + 50, 150, TextColors.argb(0x404040));
    }
}
