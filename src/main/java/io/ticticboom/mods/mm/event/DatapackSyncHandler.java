package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.StructureCategoriesSyncPkt;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class DatapackSyncHandler {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            StructureManager.validateAllPieces();
        } catch (RuntimeException t) {
            Ref.LOG.error("Error validating structure pieces on datapack sync", t);
        }
        try {
            event.getRelevantPlayers().forEach(BuildableStructureSync::send);
            var categories = new StructureCategoriesSyncPkt(StructureCategories.get(event.getPlayerList().getServer()).snapshot());
            event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, categories));
        } catch (RuntimeException t) {
            Ref.LOG.error("Error syncing builder structures on datapack sync", t);
        }
    }
}

