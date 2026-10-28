package io.ticticboom.mods.mm.client.event;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.CycleLinkerModePkt;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class LinkerScrollEvents {

    private LinkerScrollEvents() {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null || !player.isShiftKeyDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        if (!NetworkLink.isLinker(player.getMainHandItem())) {
            return;
        }
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new CycleLinkerModePkt(event.getScrollDeltaY() > 0 ? 1 : -1));
    }
}
