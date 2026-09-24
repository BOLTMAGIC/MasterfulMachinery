package io.ticticboom.mods.mm.port.fluid.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.PortGuiLayout;
import io.ticticboom.mods.mm.port.common.SlottedContainerScreen;
import io.ticticboom.mods.mm.port.fluid.FluidPortHandler;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;

/**
 * A fluid port holds one fluid type, shown as one big tank.
 */
public class FluidPortScreen extends SlottedContainerScreen<FluidPortMenu> {
    public FluidPortScreen(FluidPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private FluidPortHandler handler() {
        FluidPortBlockEntity be = menu.getBlockEntity();
        return ((FluidPortStorage) be.getStorage()).getHandler();
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        TankGauge.drawFrame(gfx, x, y);

        var handler = handler();
        Fluid fluid = handler.storedFluid();
        int amount = handler.getTotalAmount();
        int capacity = handler.getTotalCapacity();
        if (fluid != null && capacity > 0) {
            var stack = new FluidStack(fluid, amount);
            var props = IClientFluidTypeExtensions.of(fluid);
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(props.getStillTexture(stack));
            TankGauge.drawFill(gfx, x, y, (double) amount / capacity, sprite, props.getTintColor(stack));
        }
        Component name = fluid == null ? null : new FluidStack(fluid, 1).getDisplayName();
        TankGauge.drawLabel(gfx, this.font, x, y, name, amount, capacity, "mB");
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.render(gfx, mouseX, mouseY, partialTicks);
        int x = this.leftPos + PortGuiLayout.TANK_X;
        int y = this.topPos + PortGuiLayout.TANK_Y;
        if (!TankGauge.isHovered(mouseX, mouseY, x, y) || configPanel.isWithin(mouseX, mouseY)) {
            return;
        }
        var handler = handler();
        Fluid fluid = handler.storedFluid();
        var tooltip = new ArrayList<Component>();
        tooltip.add(fluid == null
                ? Component.translatable("gui.mm.port.tank.empty")
                : new FluidStack(fluid, 1).getDisplayName());
        tooltip.add(Component.literal(CountFormat.grouped(handler.getTotalAmount()) + " / "
                + CountFormat.grouped(handler.getTotalCapacity()) + " mB").withStyle(ChatFormatting.GRAY));
        Fluid locked = handler.getLockedFluid();
        if (handler.isLocked()) {
            tooltip.add(locked == null
                    ? Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.mm.port.tank.locked", new FluidStack(locked, 1).getDisplayName()).withStyle(ChatFormatting.GOLD));
        }
        gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }
}
