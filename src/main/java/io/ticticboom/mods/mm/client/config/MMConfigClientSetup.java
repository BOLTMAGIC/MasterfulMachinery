package io.ticticboom.mods.mm.client.config;

import io.ticticboom.mods.mm.net.packet.MMConfigSyncPkt;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class MMConfigClientSetup {
    private MMConfigClientSetup() {}

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new MMConfigScreen(parent));
        MMConfigSyncPkt.setClientHandler(values -> {
            io.ticticboom.mods.mm.client.tool.ToolPlacementSettings.receive(values);
            MMConfigScreen.receive(values);
        });
    }
}
