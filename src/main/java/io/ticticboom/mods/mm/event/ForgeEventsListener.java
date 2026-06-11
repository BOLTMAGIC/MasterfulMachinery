package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventsListener {

    @SubscribeEvent
    public static void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new StructureManager());
        event.addListener(new MachineRecipeManager());
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // clear any stored corners on the saver item when a player joins (world reload/start safety)
        if (event.getEntity() instanceof ServerPlayer sp) clearSavedCorners(sp);

        var structurePacket = new StructureSyncPkt(StructureManager.STRUCTURES);
        MMNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer) event.getEntity())), structurePacket);
        
        var processPacket  = new ProcessesSyncPkt(MachineRecipeManager.RECIPES);
        MMNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer) event.getEntity())), processPacket);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // clear saver tags for all currently online players on server start
        var server = event.getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            clearSavedCorners(p);
        }
    }

    private static void clearSavedCorners(ServerPlayer player) {
        var inv = player.getInventory();
        // main inventory
        for (var stack : inv.items) {
            if (stack != null && stack.getItem() == io.ticticboom.mods.mm.setup.MMRegisters.MULTIBLOCK_SAVER.get()) {
                if (stack.hasTag()) {
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS1);
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS2);
                }
            }
        }
        // offhand
        for (var stack : inv.offhand) {
            if (stack != null && stack.getItem() == io.ticticboom.mods.mm.setup.MMRegisters.MULTIBLOCK_SAVER.get()) {
                if (stack.hasTag()) {
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS1);
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS2);
                }
            }
        }
        // armour
        for (var stack : inv.armor) {
            if (stack != null && stack.getItem() == io.ticticboom.mods.mm.setup.MMRegisters.MULTIBLOCK_SAVER.get()) {
                if (stack.hasTag()) {
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS1);
                    stack.getOrCreateTag().remove(io.ticticboom.mods.mm.item.MultiblockSaverItem.NBT_POS2);
                }
            }
        }
    }

}
