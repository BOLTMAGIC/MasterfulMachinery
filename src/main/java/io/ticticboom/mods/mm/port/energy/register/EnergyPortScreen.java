package io.ticticboom.mods.mm.port.energy.register;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class EnergyPortScreen extends AbstractContainerScreen<EnergyPortMenu> {

    private final PortConfigPanel configPanel;

    public EnergyPortScreen(EnergyPortMenu menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.imageHeight = 222;
        this.imageWidth = 174;
        configPanel = new PortConfigPanel(menu.getBlockEntity());
    }

    @Override
    protected void init() {
        super.init();
        configPanel.setPosition(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (configPanel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top) && !configPanel.isWithin(mouseX, mouseY);
    }

    private long stored() {
        EnergyPortBlockEntity be = menu.getBlockEntity();
        return ((EnergyPortStorage) be.getStorage()).getStoredEnergy();
    }

    private long capacity() {
        EnergyPortBlockEntity be = menu.getBlockEntity();
        return be.getStorageModel().capacity();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(gfx, mouseX, mouseY, partialTicks);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int x = this.leftPos + TankGauge.X;
        int y = this.topPos + TankGauge.Y;
        TankGauge.drawFrame(gfx, x, y);
        long capacity = capacity();
        if (capacity > 0) {
            TankGauge.drawEnergyFill(gfx, x, y, (double) stored() / capacity);
        }
        TankGauge.drawLabel(gfx, this.font, x, y, Component.translatable("gui.mm.port.energy"), stored(), capacity, "FE");
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().displayName(), this.imageWidth);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        configPanel.extractRenderState(gfx, this.font, mouseX, mouseY);
        if (configPanel.isWithin(mouseX, mouseY)) {
            configPanel.extractTooltip(gfx, this.font, mouseX, mouseY);
            return;
        }
        extractTooltip(gfx, mouseX, mouseY);
        if (TankGauge.isHovered(mouseX, mouseY, this.leftPos + TankGauge.X, this.topPos + TankGauge.Y)) {
            gfx.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mm.port.energy"),
                    Component.literal(CountFormat.grouped(stored()) + " / " + CountFormat.grouped(capacity()) + " FE")
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }
}
