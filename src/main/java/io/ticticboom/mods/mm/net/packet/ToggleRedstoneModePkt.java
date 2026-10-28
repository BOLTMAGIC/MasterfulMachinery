package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ToggleRedstoneModePkt implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleRedstoneModePkt> TYPE =
            new CustomPacketPayload.Type<>(Ref.id("toggle_redstone_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleRedstoneModePkt> STREAM_CODEC =
            StreamCodec.of(ToggleRedstoneModePkt::encode, ToggleRedstoneModePkt::decode);

    public BlockPos pos;
    public int modeOrdinal;

    public ToggleRedstoneModePkt() {}

    public ToggleRedstoneModePkt(BlockPos pos, int modeOrdinal) {
        this.pos = pos;
        this.modeOrdinal = modeOrdinal;
    }

    public static void encode(RegistryFriendlyByteBuf buf, ToggleRedstoneModePkt pkt) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.modeOrdinal);
    }

    public static ToggleRedstoneModePkt decode(RegistryFriendlyByteBuf buf) {
        ToggleRedstoneModePkt pkt = new ToggleRedstoneModePkt();
        pkt.pos = buf.readBlockPos();
        pkt.modeOrdinal = buf.readVarInt();
        return pkt;
    }

    /**
     * IPayloadContext already dispatches to the main thread via enqueueWork and marks the packet
     * handled, so the explicit setPacketHandled call Forge required is gone. The sender is now
     * context.player() rather than a nullable getSender().
     */
    public static void handle(ToggleRedstoneModePkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            Level level = sender.level();
            if (!level.isLoaded(pkt.pos)) return;
            var be = level.getBlockEntity(pkt.pos);
            if (be instanceof MachineControllerBlockEntity mbe) {
                // simple permission check: player must be close enough
                if (sender.distanceToSqr(pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5) > 64 * 64) return;
                mbe.setRedstoneModeOrdinal(pkt.modeOrdinal);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
