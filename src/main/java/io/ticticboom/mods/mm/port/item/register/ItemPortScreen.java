package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.SlottedContainerScreen;
import io.ticticboom.mods.mm.port.item.ItemPortContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ItemPortScreen extends SlottedContainerScreen<ItemPortMenu> {

    public ItemPortScreen(ItemPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor gfx, Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        if (x + 16 <= 0 || y + 16 <= 0 || x >= this.width || y >= this.height) {
            return;
        }
        if (!(slot.container instanceof ItemPortContainer port)) {
            super.extractSlot(gfx, slot, mouseX, mouseY);
            return;
        }
        var stack = port.getHandler().getStackInSlot(slot.getContainerSlot());
        if (stack.isEmpty()) {
            return;
        }
        gfx.item(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
        if (stack.getCount() >= 100) {
            gfx.itemDecorations(this.font, stack, slot.x, slot.y, "");
            CountFormat.drawSlotCount(gfx, slot.x - 1, slot.y - 1, stack.getCount());
        } else {
            gfx.itemDecorations(this.font, stack, slot.x, slot.y);
        }
    }
}
