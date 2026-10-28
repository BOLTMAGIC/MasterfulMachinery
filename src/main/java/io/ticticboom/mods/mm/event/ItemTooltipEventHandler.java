package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.port.IPortItem;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT)
public class ItemTooltipEventHandler {
    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof IPortItem pi) {
            Component typeName = pi.getTypeName();
            event.getToolTip().add(1, typeName);
        }
    }
}
