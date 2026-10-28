package io.ticticboom.mods.mm.client.structure;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class StructureViewScrollHandler {

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (Minecraft.getInstance().screen == null) {
            return;
        }
        var hovered = GuiStructureRenderer.getHovered();
        if (hovered == null) {
            return;
        }
        hovered.scroll(event.getScrollDeltaY());
        event.setCanceled(true);
    }
}
