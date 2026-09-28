package io.ticticboom.mods.mm.client.config;

import io.ticticboom.mods.mm.net.packet.MMConfigSyncPkt;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only hooks for the Mods menu Config button and server setting snapshots. */
public final class MMConfigClientSetup {
    private MMConfigClientSetup() {}

    public static void register() {
        @SuppressWarnings("removal") var context = ModLoadingContext.get();
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(MMConfigScreen::new));
        MMConfigSyncPkt.setClientHandler(MMConfigScreen::receive);
    }
}
