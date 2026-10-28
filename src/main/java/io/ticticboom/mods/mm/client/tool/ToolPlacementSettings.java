package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.Map;

/** The server's Structure Builder range on this client, for the hologram; synced with the MM config. */
@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ToolPlacementSettings {
    private static int range = 64;

    private ToolPlacementSettings() {}

    public static int range() { return range; }

    public static void receive(Map<String, String> values) {
        try {
            range = Math.max(5, Math.min(128, Integer.parseInt(values.getOrDefault("tool.buildRange", "64"))));
        } catch (NumberFormatException e) { range = 64; }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { range = 64; }
}
