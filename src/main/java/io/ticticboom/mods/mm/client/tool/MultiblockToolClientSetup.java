package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only registration for the tool's menu screen and the dismantle key's name in its tooltip. Kept out of the common {@code SetupEventHandler}
 * (which loads on the dedicated server too) so no client class is ever referenced from common code.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MultiblockToolClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MMRegisters.MULTIBLOCK_TOOL_MENU.get(), MultiblockToolScreen::new));
        MultiblockToolItem.setDismantleKeyName(ToolKeys.DISMANTLE::getTranslatedKeyMessage);
    }
}
