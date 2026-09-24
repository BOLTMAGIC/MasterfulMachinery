package io.ticticboom.mods.mm.port.energy.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorageModel;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

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

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int p_282681_, int p_283686_) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().name());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + 7, this.topPos + 50, 89, 78, 162, 80);
        EnergyPortBlockEntity be = menu.getBlockEntity();
        EnergyPortStorage storage = (EnergyPortStorage) be.getStorage();
        EnergyPortStorageModel storageModel = be.getStorageModel();
        var filledValue = (double)storage.getStoredEnergy() / (double)storageModel.capacity();
        var filledHeight = (int)(Math.min(filledValue, 1) * 78);
        var start = 129 - filledHeight;
        gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + 8, this.topPos + start, 90, 0, 160, filledHeight);
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + 7, this.topPos + 50, 162, 80)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.literal(String.format("Storage Energy: %sFE / %sFE", storage.getStoredEnergy(), storageModel.capacity())));
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
        configPanel.render(gfx, this.font, mouseX, mouseY);
        configPanel.renderTooltip(gfx, this.font, mouseX, mouseY);
    }
}