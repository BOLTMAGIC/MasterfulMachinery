package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DatapackSyncHandler {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            StructureManager.validateAllPieces();
        } catch (Throwable t) {
            Ref.LOG.error("Error validating structure pieces on datapack sync", t);
        }
        try {
            // fired for a joining player and, with no player, for everyone after /reload
            BuildableStructureSync.send(event.getPlayer() != null
                    ? PacketDistributor.PLAYER.with(event::getPlayer)
                    : PacketDistributor.ALL.noArg());
        } catch (Throwable t) {
            Ref.LOG.error("Error syncing builder structures on datapack sync", t);
        }
    }
}

