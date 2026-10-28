package io.ticticboom.mods.mm.net;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.AssemblyPkt;
import io.ticticboom.mods.mm.net.packet.OpenMachineScreenPkt;
import io.ticticboom.mods.mm.net.packet.BuildableStructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.ToolDismantlePkt;
import io.ticticboom.mods.mm.net.packet.ToolHudPkt;
import io.ticticboom.mods.mm.net.packet.ToolRotatePkt;
import io.ticticboom.mods.mm.net.packet.ToolAnchorPkt;
import io.ticticboom.mods.mm.net.packet.ToolBuildPkt;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt;
import io.ticticboom.mods.mm.net.packet.CycleLinkerModePkt;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.SelectRecipePkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoryEditPkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoriesSyncPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigRequestPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigEditPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigSyncPkt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge replaced Forge's SimpleChannel with typed payloads. Each packet now carries its own
 * CustomPacketPayload.Type and StreamCodec, registration happens on an event rather than in the
 * mod constructor, and the direction is declared up front instead of being implied by the
 * sending code. The protocol version is still negotiated, so mismatched clients are rejected.
 */
@EventBusSubscriber(modid = Ref.ID)
public class MMNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StructureSyncPkt.TYPE, StructureSyncPkt.STREAM_CODEC, StructureSyncPkt::handle);
        registrar.playToClient(ProcessesSyncPkt.TYPE, ProcessesSyncPkt.STREAM_CODEC, ProcessesSyncPkt::handle);
        registrar.playToServer(ToggleRedstoneModePkt.TYPE, ToggleRedstoneModePkt.STREAM_CODEC, ToggleRedstoneModePkt::handle);
        registrar.playToServer(OpenMachineScreenPkt.TYPE, OpenMachineScreenPkt.STREAM_CODEC, OpenMachineScreenPkt::handle);
        registrar.playToServer(SelectRecipePkt.TYPE, SelectRecipePkt.STREAM_CODEC, SelectRecipePkt::handle);
        registrar.playToServer(ControllerSettingsPkt.TYPE, ControllerSettingsPkt.STREAM_CODEC, ControllerSettingsPkt::handle);
        registrar.playToServer(CycleLinkerModePkt.TYPE, CycleLinkerModePkt.STREAM_CODEC, CycleLinkerModePkt::handle);
        registrar.playToServer(ToolRotatePkt.TYPE, ToolRotatePkt.STREAM_CODEC, ToolRotatePkt::handle);
        registrar.playToServer(ToolDismantlePkt.TYPE, ToolDismantlePkt.STREAM_CODEC, ToolDismantlePkt::handle);
        registrar.playToServer(ToolSettingsPkt.TYPE, ToolSettingsPkt.STREAM_CODEC, ToolSettingsPkt::handle);
        registrar.playToClient(ToolHudPkt.TYPE, ToolHudPkt.STREAM_CODEC, ToolHudPkt::handle);
        registrar.playToClient(BuildableStructureSyncPkt.TYPE, BuildableStructureSyncPkt.STREAM_CODEC, BuildableStructureSyncPkt::handle);
        registrar.playToServer(AssemblyPkt.TYPE, AssemblyPkt.STREAM_CODEC, AssemblyPkt::handle);
        registrar.playToServer(PortConfigPkt.TYPE, PortConfigPkt.STREAM_CODEC, PortConfigPkt::handle);
        registrar.playToClient(StructureCategoriesSyncPkt.TYPE, StructureCategoriesSyncPkt.STREAM_CODEC, StructureCategoriesSyncPkt::handle);
        registrar.playToServer(StructureCategoryEditPkt.TYPE, StructureCategoryEditPkt.STREAM_CODEC, StructureCategoryEditPkt::handle);
        registrar.playToServer(MMConfigRequestPkt.TYPE, MMConfigRequestPkt.STREAM_CODEC, MMConfigRequestPkt::handle);
        registrar.playToServer(MMConfigEditPkt.TYPE, MMConfigEditPkt.STREAM_CODEC, MMConfigEditPkt::handle);
        registrar.playToClient(MMConfigSyncPkt.TYPE, MMConfigSyncPkt.STREAM_CODEC, MMConfigSyncPkt::handle);
        registrar.playToServer(ToolBuildPkt.TYPE, ToolBuildPkt.STREAM_CODEC, ToolBuildPkt::handle);
        registrar.playToServer(ToolAnchorPkt.TYPE, ToolAnchorPkt.STREAM_CODEC, ToolAnchorPkt::handle);
    }
}
