package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoriesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.StructureCategories;
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
        // fired for a joining player and, with no player, for everyone after /reload.
        // Forge posts it before the vanilla recipe/tag packets, which is what makes JEI build its recipe list,
        // so these must go out here and not in PlayerLoggedInEvent (that one fires after JEI has already loaded).
        PacketDistributor.PacketTarget target = event.getPlayer() != null
                ? PacketDistributor.PLAYER.with(event::getPlayer)
                : PacketDistributor.ALL.noArg();
        try {
            MMNetwork.INSTANCE.send(target, new StructureSyncPkt(StructureManager.STRUCTURES));
            MMNetwork.INSTANCE.send(target, new ProcessesSyncPkt(MachineRecipeManager.RECIPES));
        } catch (Throwable t) {
            Ref.LOG.error("Error syncing structures and recipes on datapack sync", t);
        }
        try {
            BuildableStructureSync.send(target);
            MMNetwork.INSTANCE.send(target,
                    new StructureCategoriesSyncPkt(StructureCategories.get(event.getPlayer() != null
                            ? event.getPlayer().getServer() : event.getPlayerList().getServer()).snapshot()));
        } catch (Throwable t) {
            Ref.LOG.error("Error syncing builder structures on datapack sync", t);
        }
    }
}

