package io.ticticboom.mods.mm.event;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Ref.ID)
public class ControllerInvalidationListener {

    @SubscribeEvent
    public static void onBlockBroken(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldUtil.scheduleNearbyValidation(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldUtil.scheduleNearbyValidation(level, event.getPos());
        }
    }
}
