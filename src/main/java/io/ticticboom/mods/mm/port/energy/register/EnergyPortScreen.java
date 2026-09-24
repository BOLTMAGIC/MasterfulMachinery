package io.ticticboom.mods.mm.port.energy.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.PortGuiLayout;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorageModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Energy port: one big gauge in the same frame and place as the fluid / chemical tanks, filled with
 * MM's energy bar texture, with the stored amount written on it.
 */
public class EnergyPortScreen extends AbstractContainerScreen<EnergyPortMenu> {

    private final PortConfigPanel configPanel;

    public EnergyPortScreen(EnergyPortMenu menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.imageHeight = PortGuiLayout.HEIGHT;
        this.imageWidth = PortGuiLayout.WIDTH;
        configPanel = new PortConfigPanel(menu.getBlockEntity());
    }

    @Override
    protected void init() {
        super.init();
        configPanel.setPosition(this.leftPos, this.topPos);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configPanel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !configPanel.isWithin(mouseX, mouseY);
    }

    private long stored() {
        EnergyPortBlockEntity be = menu.getBlockEntity();
        return ((EnergyPortStorage) be.getStorage()).getStoredEnergy();
    }

    private long capacity() {
        EnergyPortBlockEntity be = menu.getBlockEntity();
        EnergyPortStorageModel model = be.getStorageModel();
        return model.capacity();
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        TankGauge.drawFrame(gfx, x, y);
        long capacity = capacity();
        if (capacity > 0) {
            TankGauge.drawEnergyFill(gfx, x, y, (double) stored() / capacity);
        }
        TankGauge.drawLabel(gfx, this.font, x, y, Component.translatable("gui.mm.port.energy"), stored(), capacity, "FE");
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().name());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        configPanel.render(gfx, this.font, mouseX, mouseY);
        renderTooltip(gfx, mouseX, mouseY);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        if (TankGauge.isHovered(mouseX, mouseY, x, y) && !configPanel.isWithin(mouseX, mouseY)) {
            gfx.renderComponentTooltip(this.font, List.of(Component.translatable("gui.mm.port.energy"),
                    Component.literal(CountFormat.grouped(stored()) + " / " + CountFormat.grouped(capacity()) + " FE")
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
        configPanel.renderTooltip(gfx, this.font, mouseX, mouseY);
    }
}
