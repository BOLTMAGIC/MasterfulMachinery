package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.Ref;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = Ref.ID)
public class MMConfigSetup {
    public static final MMCommonConfig COMMON;
    private static final ForgeConfigSpec commonSpec;
    public static final MMClientConfig CLIENT;
    private static final ForgeConfigSpec clientSpec;

    static {
        final Pair<MMCommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(MMCommonConfig::new);
        COMMON = specPair.getKey();
        commonSpec = specPair.getRight();
        final Pair<MMClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(MMClientConfig::new);
        CLIENT = clientPair.getKey();
        clientSpec = clientPair.getRight();
    }

    public static void setup() {
        @SuppressWarnings("removal")
        var ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, commonSpec);
        ctx.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }

    @SubscribeEvent
    public static void on(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            MMConfig.bake();
        }
    }
}
