package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;

public class SlottedContainerScreen<T extends AbstractContainerMenu & IPortMenu> extends AbstractContainerScreen<T> {

    protected final T menu;
    protected ArrayList<Vec2> slots = new ArrayList<>();
    protected final PortConfigPanel configPanel;

    public SlottedContainerScreen(T menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.menu = menu;
        IPortBlockEntity portBe = menu.getBlockEntity();
        var model = (ISlottedPortStorageModel) portBe.getStorage().getStorageModel();
        this.imageWidth = PortGuiLayout.width(model.columns());
        this.imageHeight = PortGuiLayout.HEIGHT + PortGuiLayout.extraHeight(model.rows());
        configPanel = new PortConfigPanel(menu.getBlockEntity());
        setupSlots();
    }

    @Override
    protected void init() {
        super.init();
        configPanel.setPosition(this.leftPos, this.topPos, this.imageWidth);
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

    private void setupSlots() {
        IPortBlockEntity blockEntity = menu.getBlockEntity();
        var storage = blockEntity.getStorage();
        var model = (ISlottedPortStorageModel) storage.getStorageModel();

        var columns = model.columns();
        var rows = model.rows();

        // slot backgrounds are drawn one pixel up-left of the menu's slot positions
        int offsetX = PortGuiLayout.slotGridX(columns) - 1;
        int offsetY = PortGuiLayout.slotGridY(rows) - 1;
        slots.ensureCapacity(columns * rows);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                slots.add(new Vec2(x * 18 + offsetX, y * 18 + offsetY));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        if (this.imageWidth == PortGuiLayout.WIDTH && this.imageHeight == PortGuiLayout.HEIGHT) {
            gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        } else {
            // grid larger than 9x6: stretch the window, then copy the player inventory from the standard
            // background, shifted like the menu's inventory slots
            gfx.blitNineSlicedSized(Ref.UiTextures.TILING_GUI, this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                    4, 4, 4, 4, 12, 12, 0, 0, 12, 12);
            int invX = this.leftPos + (this.imageWidth - PortGuiLayout.WIDTH) / 2;
            int invY = this.topPos + this.imageHeight - PortGuiLayout.HEIGHT;
            gfx.blit(Ref.UiTextures.PORT_GUI, invX + 7, invY + 139, 7, 139, 162, 78);
        }
        for (Vec2 slot : slots) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + (int) slot.x, this.topPos + (int) slot.y, 0, 26, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().name(), this.imageWidth);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTicks);
        configPanel.render(gfx, this.font, mouseX, mouseY);
        renderTooltip(gfx, mouseX, mouseY);
        configPanel.renderTooltip(gfx, this.font, mouseX, mouseY);
    }
}
