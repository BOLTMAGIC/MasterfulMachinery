package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
