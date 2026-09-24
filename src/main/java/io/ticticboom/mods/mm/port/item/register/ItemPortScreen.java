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
        var stack = slot.getItem();
        if (!(slot.container instanceof ItemPortContainer) || stack.getCount() < 100) {
            super.renderSlot(gfx, slot);
            return;
        }
        gfx.renderItem(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
        // empty count text: durability bar / cooldown only, the count is drawn below
        gfx.renderItemDecorations(this.font, stack, slot.x, slot.y, "");
        CountFormat.drawSlotCount(gfx, slot.x - 1, slot.y - 1, stack.getCount());
    }
}
