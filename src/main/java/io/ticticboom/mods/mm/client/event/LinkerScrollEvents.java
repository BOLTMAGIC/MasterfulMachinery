package io.ticticboom.mods.mm.client.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.CycleLinkerModePkt;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class LinkerScrollEvents {

    private LinkerScrollEvents() {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null || !player.isShiftKeyDown() || event.getScrollDelta() == 0) {
            return;
        }
        if (!NetworkLink.isLinker(player.getMainHandItem())) {
            return;
        }
        // sneak + wheel changes the linker's mode instead of the hotbar slot
        event.setCanceled(true);
        MMNetwork.INSTANCE.sendToServer(new CycleLinkerModePkt(event.getScrollDelta() > 0 ? 1 : -1));
    }
}
