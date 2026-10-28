package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


@EventBusSubscriber(value = Dist.CLIENT)
public class BlueprintScreenEvents {

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!MMConfig.PREVIEW_BP_SCREEN) {
            return;
        }

        if (!event.getItemStack().is(MMRegisters.BLUEPRINT.get())) {
            return;
        }

        Level level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }

        Minecraft.getInstance().setScreen(new StructureBlueprintScreen());
    }
}
