package io.ticticboom.mods.mm.net;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.BuildableStructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.CycleLinkerModePkt;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.ToolHudPkt;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class MMNetwork {

    // 4: tool ME settings actions and the tool HUD packet; 5: builder structure sync
    private static final String PROTOCOL_VERSION = "5";
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
    }
}
