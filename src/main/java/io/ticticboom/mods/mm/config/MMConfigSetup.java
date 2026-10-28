package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = Ref.ID)
public class MMConfigSetup {
    public static final MMCommonConfig COMMON;
    private static final ModConfigSpec commonSpec;
    public static final MMClientConfig CLIENT;
    private static final ModConfigSpec clientSpec;

    static {
        final Pair<MMCommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(MMCommonConfig::new);
        COMMON = specPair.getKey();
        commonSpec = specPair.getRight();
        final Pair<MMClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(MMClientConfig::new);
        CLIENT = clientPair.getKey();
        clientSpec = clientPair.getRight();
    }

    // NeoForge removed ModLoadingContext.registerConfig; the mod container handed to the @Mod
    // constructor owns config registration now.
    public static void setup(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, commonSpec);
        container.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }

    @SubscribeEvent
    public static void on(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            MMConfig.bake();
        }
    }
}
