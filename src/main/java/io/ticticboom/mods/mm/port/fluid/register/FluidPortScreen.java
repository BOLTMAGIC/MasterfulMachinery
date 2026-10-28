package io.ticticboom.mods.mm.port.fluid.register;

import io.ticticboom.mods.mm.client.FluidSprites;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.SlottedContainerScreen;
import io.ticticboom.mods.mm.port.fluid.FluidPortHandler;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FluidPortScreen extends SlottedContainerScreen<FluidPortMenu> {
    public FluidPortScreen(FluidPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private FluidPortHandler handler() {
        FluidPortBlockEntity be = menu.getBlockEntity();
        return ((FluidPortStorage) be.getStorage()).getHandler();
    }

    private boolean singleTank() {
        return handler().getTanks() == 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(gfx, mouseX, mouseY, partialTicks);
        if (!singleTank()) {
            super.renderBg(gfx, partialTicks, mouseX, mouseY);
            return;
        }
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int x = this.leftPos + TankGauge.X;
        int y = this.topPos + TankGauge.Y;
        TankGauge.drawFrame(gfx, x, y);
        var handler = handler();
        var stack = handler.getFluidInTank(0);
        int capacity = handler.getTankCapacity(0);
        if (!stack.isEmpty() && capacity > 0) {
            TankGauge.drawFill(gfx, x, y, (double) stack.getAmount() / capacity, FluidSprites.still(stack), FluidSprites.tint(stack));
        }
        TankGauge.drawLabel(gfx, this.font, x, y, stack.isEmpty() ? null : stack.getHoverName(), stack.getAmount(), capacity, "mB");
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        super.renderLabels(gfx, mouseX, mouseY);
        if (singleTank()) {
            return;
        }
        int i = firstSlotOnPage();
        for (Vec2 slot : slots) {
            int slotX = (int) slot.x + 1;
            int slotY = (int) slot.y + 1;
            var stack = menu.getStackInSlot(i);
            FluidRenderer.INSTANCE.extractRenderState(gfx, slotX, slotY, stack, 16);
            if (!stack.isEmpty()) {
                CountFormat.drawSlotCount(gfx, slotX - 1, slotY - 1, stack.getAmount());
            }
            if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + slotX, this.topPos + slotY, 16, 16)) {
                gfx.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 0x80ffffff, 0x80ffffff);
            }
            i++;
        }
    }

    @Override
    protected void renderExtraTooltip(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        var handler = handler();
        if (singleTank()) {
            if (TankGauge.isHovered(mouseX, mouseY, this.leftPos + TankGauge.X, this.topPos + TankGauge.Y)) {
                gfx.setComponentTooltipForNextFrame(this.font, tankTooltip(handler, 0), mouseX, mouseY);
            }
            return;
        }
        int i = firstSlotOnPage();
        for (Vec2 slot : slots) {
            if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + (int) slot.x + 1, this.topPos + (int) slot.y + 1, 16, 16)) {
                gfx.setComponentTooltipForNextFrame(this.font, tankTooltip(handler, i), mouseX, mouseY);
                return;
            }
            i++;
        }
    }

    private static List<Component> tankTooltip(FluidPortHandler handler, int tank) {
        var stack = handler.getFluidInTank(tank);
        var tooltip = new ArrayList<Component>();
        tooltip.add(stack.isEmpty() ? Component.translatable("gui.mm.port.tank.empty") : stack.getHoverName());
        tooltip.add(Component.literal(CountFormat.grouped(stack.getAmount()) + " / "
                + CountFormat.grouped(handler.getTankCapacity(tank)) + " mB").withStyle(ChatFormatting.GRAY));
        if (handler.isLocked()) {
            var locked = handler.getLockedFluid(tank);
            tooltip.add(locked == null
                    ? Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.mm.port.tank.locked", new FluidStack(locked, 1).getHoverName()).withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
