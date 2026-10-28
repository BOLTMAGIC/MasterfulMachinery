package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.ToolHudPkt;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class MultiblockToolClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MMRegisters.MULTIBLOCK_TOOL_MENU.get(), MultiblockToolScreen::new));
        MultiblockToolItem.setDismantleKeyName(ToolKeys.DISMANTLE::getTranslatedKeyMessage);
        MultiblockToolItem.setAnchorKeyName(ToolKeys.ANCHOR::getTranslatedKeyMessage);
        ToolHudPkt.setClientHandler(ToolHudOverlay::receive);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE, ToolHudOverlay.ID, ToolHudOverlay.OVERLAY);
    }
}
