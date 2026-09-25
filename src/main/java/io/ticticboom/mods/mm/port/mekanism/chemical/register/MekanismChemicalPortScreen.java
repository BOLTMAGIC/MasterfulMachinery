package io.ticticboom.mods.mm.port.mekanism.chemical.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.PortGuiLayout;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class MekanismChemicalPortScreen<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, T extends MekanismChemicalPortMenu<CHEMICAL, STACK>> extends AbstractContainerScreen<T> {

    protected final MekanismChemicalPortBlockEntity<CHEMICAL, STACK> be;
    protected final MekanismChemicalPortStorage<CHEMICAL, STACK> storage;
    private final PortConfigPanel configPanel;


    public MekanismChemicalPortScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = PortGuiLayout.WIDTH;
        be = this.menu.getBlockEntity();
        storage = (MekanismChemicalPortStorage<CHEMICAL, STACK>) be.getStorage();
        configPanel = new PortConfigPanel(be);
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
    protected void renderBg(GuiGraphics gfx, float v, int i, int i1) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        TankGauge.drawFrame(gfx, x, y);

        var stack = storage.chemicalTank.getStack();
        long capacity = storage.chemicalTank.getCapacity();
        if (!stack.isEmpty() && capacity > 0) {
            TankGauge.drawFill(gfx, x, y, (double) stack.getAmount() / capacity,
                    MekanismRenderer.getSprite(stack.getType().getIcon()), stack.getType().getTint());
        }
        Component name = stack.isEmpty() ? null : stack.getType().getTextComponent();
        TankGauge.drawLabel(gfx, this.font, x, y, name, stack.getAmount(), capacity, "mB");
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().name());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        configPanel.render(gfx, this.font, mouseX, mouseY);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        if (TankGauge.isHovered(mouseX, mouseY, x, y) && !configPanel.isWithin(mouseX, mouseY)) {
            var stack = storage.chemicalTank.getStack();
            var tooltip = new ArrayList<Component>();
            tooltip.add(stack.isEmpty() ? Component.translatable("gui.mm.port.tank.empty") : stack.getType().getTextComponent());
            tooltip.add(Component.literal(CountFormat.grouped(stack.getAmount()) + " / "
                    + CountFormat.grouped(storage.chemicalTank.getCapacity()) + " mB").withStyle(ChatFormatting.GRAY));
            if (storage.isLocked()) {
                tooltip.add(Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD));
            }
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
        configPanel.renderTooltip(gfx, this.font, mouseX, mouseY);
    }
}