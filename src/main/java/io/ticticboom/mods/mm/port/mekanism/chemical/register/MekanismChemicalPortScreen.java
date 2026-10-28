package io.ticticboom.mods.mm.port.mekanism.chemical.register;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class MekanismChemicalPortScreen<T extends MekanismChemicalPortMenu> extends AbstractContainerScreen<T> {

    protected final MekanismChemicalPortBlockEntity be;
    protected final MekanismChemicalPortStorage storage;
    private final PortConfigPanel configPanel;

    public MekanismChemicalPortScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = 174;
        be = this.menu.getBlockEntity();
        storage = (MekanismChemicalPortStorage) be.getStorage();
        configPanel = new PortConfigPanel(be);
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
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !configPanel.isWithin(mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int x = this.leftPos + TankGauge.X;
        int y = this.topPos + TankGauge.Y;
        TankGauge.drawFrame(gfx, x, y);
        var stack = storage.chemicalTank.getStack();
        long capacity = storage.chemicalTank.getCapacity();
        if (!stack.isEmpty() && capacity > 0) {
            TankGauge.drawFill(gfx, x, y, (double) stack.getAmount() / capacity,
                    MekanismRenderer.getSprite(stack.getChemical().getIcon()), stack.getChemical().getTint());
        }
        Component name = stack.isEmpty() ? null : stack.getChemical().getTextComponent();
        TankGauge.drawLabel(gfx, this.font, x, y, name, stack.getAmount(), capacity, "mB");
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
            configPanel.setTooltipForNextFrame(gfx, this.font, mouseX, mouseY);
            return;
        }
        extractTooltip(gfx, mouseX, mouseY);
        if (TankGauge.isHovered(mouseX, mouseY, this.leftPos + TankGauge.X, this.topPos + TankGauge.Y)) {
            var stack = storage.chemicalTank.getStack();
            var tooltip = new ArrayList<Component>();
            tooltip.add(stack.isEmpty() ? Component.translatable("gui.mm.port.tank.empty") : stack.getChemical().getTextComponent());
            tooltip.add(Component.literal(CountFormat.grouped(stack.getAmount()) + " / "
                    + CountFormat.grouped(storage.chemicalTank.getCapacity()) + " mB").withStyle(ChatFormatting.GRAY));
            if (storage.isLocked()) {
                tooltip.add(Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD));
            }
            gfx.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }
}
