package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.config.MMConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlueprintScreenEvents {

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!MMConfig.PREVIEW_BP_SCREEN) {
            return;
        }

        Level level = event.getLevel();
        if (!level.isClientSide) {
            return;
        }

        Minecraft.getInstance().setScreen(new StructureBlueprintScreen());
    }
}
