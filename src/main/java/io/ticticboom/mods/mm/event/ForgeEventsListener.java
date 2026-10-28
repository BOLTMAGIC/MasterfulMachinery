package io.ticticboom.mods.mm.event;

import net.minecraft.world.entity.EquipmentSlot;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureLoader;
import io.ticticboom.mods.mm.util.ItemNbtUtil;

import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.server.level.ServerPlayer;
import io.ticticboom.mods.mm.util.ItemComponentUtil;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import io.ticticboom.mods.mm.item.MultiblockSaverItem;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.Ref;

@EventBusSubscriber
public class ForgeEventsListener {

    @SubscribeEvent
    public static void registerReloadListeners(AddServerReloadListenersEvent event) {
        ItemComponentUtil.setRegistries(event.getRegistryAccess());
        event.addListener(Ref.id("structures"), new StructureManager());
        event.addListener(Ref.id("recipes"), new MachineRecipeManager());
        event.addListener(Ref.id("buildable_structures"), new BuildableStructureLoader());
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // clear any stored corners on the saver item when a player joins (world reload/start safety)
        if (event.getEntity() instanceof ServerPlayer sp) clearSavedCorners(sp);

        var structurePacket = new StructureSyncPkt(StructureManager.STRUCTURES);
        PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(), structurePacket);
        
        var processPacket  = new ProcessesSyncPkt(MachineRecipeManager.RECIPES);
        PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(), processPacket);
        // the Structure Builder's range and placement settings, before the player uses it
        PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(),
                new io.ticticboom.mods.mm.net.packet.MMConfigSyncPkt(io.ticticboom.mods.mm.config.MMConfigOptions.serverSnapshot()));
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
        for (var stack : inv.getNonEquipmentItems()) {
            if (stack != null && stack.getItem() == MMRegisters.MULTIBLOCK_SAVER.get()) {
                if (ItemNbtUtil.hasTag(stack)) {
                    ItemNbtUtil.mutate(stack, __t -> __t.remove(MultiblockSaverItem.NBT_POS1));
                    ItemNbtUtil.mutate(stack, __t -> __t.remove(MultiblockSaverItem.NBT_POS2));
                }
            }
        }
        // offhand and armour
        for (var slot : EquipmentSlot.values()) {
            var stack = player.getItemBySlot(slot);
            if (stack.getItem() == MMRegisters.MULTIBLOCK_SAVER.get() && ItemNbtUtil.hasTag(stack)) {
                ItemNbtUtil.mutate(stack, __t -> __t.remove(MultiblockSaverItem.NBT_POS1));
                ItemNbtUtil.mutate(stack, __t -> __t.remove(MultiblockSaverItem.NBT_POS2));
            }
        }
    }

}
