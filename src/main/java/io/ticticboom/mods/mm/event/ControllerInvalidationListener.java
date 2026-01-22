package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Ref.ID)
public class ControllerInvalidationListener {

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        var la = event.getLevel();
        if (!(la instanceof Level level)) return;
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;
        BlockPos pos = event.getPos();
        var controllers = WorldUtil.findControllerBlockEntitiesInRadius(pos, sl, 6);
        for (var cbe : controllers) {
            try {
                if (cbe instanceof io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity mc) {
                    mc.invalidateProgress();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        var la = event.getLevel();
        if (!(la instanceof Level level)) return;
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;
        BlockPos pos = event.getPos();
        var controllers = WorldUtil.findControllerBlockEntitiesInRadius(pos, sl, 6);
        for (var cbe : controllers) {
            try {
                if (cbe instanceof io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity mc) {
                    // trigger a re-check; we call validateStructure via server thread by setting formed=false and letting tick revalidate,
                    mc.requestValidation();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
