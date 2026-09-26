package io.ticticboom.mods.mmtest;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Registers the GameTests that need AE2 ({@link MeToolGameTests}) only when AE2 is installed, so the suite also loads
 * and runs without it ({@code -PnoAe2}); the class is never touched otherwise.
 */
@Mod.EventBusSubscriber(modid = "mmtest", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MeGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MeGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("ae2")) {
            event.register(MeToolGameTests.class);
        } else {
            LOGGER.info("AE2 is not installed: skipping the ME GameTests");
        }
    }
}
