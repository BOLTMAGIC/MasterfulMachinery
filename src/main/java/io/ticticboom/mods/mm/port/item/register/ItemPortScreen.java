package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.common.SlottedContainerScreen;
import io.ticticboom.mods.mm.port.item.ItemPortContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ItemPortScreen extends SlottedContainerScreen<ItemPortMenu> {

    public ItemPortScreen(ItemPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

    }

    /**
     * Port slots can hold more than a stack; counts above two digits are drawn compact (1.2K) so
     * they stay inside the slot, like MM's JEI categories.
     */
    @Override
    protected void renderSlot(GuiGraphics gfx, Slot slot) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        if (x + 16 <= 0 || y + 16 <= 0 || x >= this.width || y >= this.height) return;
        if (!(slot.container instanceof ItemPortContainer port)) {
            super.renderSlot(gfx, slot);
            return;
        }
        // Rendering is read-only. The container's getItem copies every large stack;
        // avoid that allocation for every visible port slot on every frame.
        var stack = port.getHandler().getStackInSlot(slot.getContainerSlot());
        if (stack.isEmpty()) return;
        gfx.renderItem(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
        if (stack.getCount() >= 100) {
            gfx.renderItemDecorations(this.font, stack, slot.x, slot.y, "");
            CountFormat.drawSlotCount(gfx, slot.x - 1, slot.y - 1, stack.getCount());
        } else {
            gfx.renderItemDecorations(this.font, stack, slot.x, slot.y);
        }
    }
}
