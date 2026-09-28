package io.ticticboom.mods.mm.net;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.BuildableStructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.CycleLinkerModePkt;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoryEditPkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoriesSyncPkt;
import io.ticticboom.mods.mm.net.packet.ToolHudPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigRequestPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigEditPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigSyncPkt;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class MMNetwork {

    // 7: in-game MM config request, edit and sync
    private static final String PROTOCOL_VERSION = "7";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Ref.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );


    public static void init() {
        int index = 0;
        INSTANCE.registerMessage(index++, StructureSyncPkt.class, StructureSyncPkt::encode, StructureSyncPkt::decode, StructureSyncPkt::handle);
        INSTANCE.registerMessage(index++, ProcessesSyncPkt.class, ProcessesSyncPkt::encode, ProcessesSyncPkt::decode, ProcessesSyncPkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt.class,
                io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt::encode,
                io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt::decode,
                io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt::handle);
        INSTANCE.registerMessage(index++, PortConfigPkt.class, PortConfigPkt::encode, PortConfigPkt::decode, PortConfigPkt::handle);
        INSTANCE.registerMessage(index++, CycleLinkerModePkt.class, CycleLinkerModePkt::encode, CycleLinkerModePkt::decode, CycleLinkerModePkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt.class,
                io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt::encode,
                io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt::decode,
                io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.AssemblyPkt.class,
                io.ticticboom.mods.mm.net.packet.AssemblyPkt::encode,
                io.ticticboom.mods.mm.net.packet.AssemblyPkt::decode,
                io.ticticboom.mods.mm.net.packet.AssemblyPkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.ToolRotatePkt.class,
                io.ticticboom.mods.mm.net.packet.ToolRotatePkt::encode,
                io.ticticboom.mods.mm.net.packet.ToolRotatePkt::decode,
                io.ticticboom.mods.mm.net.packet.ToolRotatePkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.ToolDismantlePkt.class,
                io.ticticboom.mods.mm.net.packet.ToolDismantlePkt::encode,
                io.ticticboom.mods.mm.net.packet.ToolDismantlePkt::decode,
                io.ticticboom.mods.mm.net.packet.ToolDismantlePkt::handle);
        INSTANCE.registerMessage(index++, io.ticticboom.mods.mm.net.packet.ToolSettingsPkt.class,
                io.ticticboom.mods.mm.net.packet.ToolSettingsPkt::encode,
                io.ticticboom.mods.mm.net.packet.ToolSettingsPkt::decode,
                io.ticticboom.mods.mm.net.packet.ToolSettingsPkt::handle);
        INSTANCE.registerMessage(index++, ToolHudPkt.class, ToolHudPkt::encode, ToolHudPkt::decode, ToolHudPkt::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(index++, BuildableStructureSyncPkt.class, BuildableStructureSyncPkt::encode,
                BuildableStructureSyncPkt::decode, BuildableStructureSyncPkt::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(index++, StructureCategoriesSyncPkt.class, StructureCategoriesSyncPkt::encode,
                StructureCategoriesSyncPkt::decode, StructureCategoriesSyncPkt::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(index++, StructureCategoryEditPkt.class, StructureCategoryEditPkt::encode,
                StructureCategoryEditPkt::decode, StructureCategoryEditPkt::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(index++, MMConfigRequestPkt.class, MMConfigRequestPkt::encode,
                MMConfigRequestPkt::decode, MMConfigRequestPkt::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(index++, MMConfigEditPkt.class, MMConfigEditPkt::encode,
                MMConfigEditPkt::decode, MMConfigEditPkt::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(index++, MMConfigSyncPkt.class, MMConfigSyncPkt::encode,
                MMConfigSyncPkt::decode, MMConfigSyncPkt::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
