package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMachineScreenPkt(BlockPos pos, boolean controller) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMachineScreenPkt> TYPE = new CustomPacketPayload.Type<>(Ref.id("open_machine_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMachineScreenPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos());
                buf.writeBoolean(pkt.controller());
            },
            buf -> new OpenMachineScreenPkt(buf.readBlockPos(), buf.readBoolean()));

    public static void handle(OpenMachineScreenPkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.level().isLoaded(pkt.pos())) {
                return;
            }
            if (player.distanceToSqr(pkt.pos().getX() + 0.5, pkt.pos().getY() + 0.5, pkt.pos().getZ() + 0.5) > 64) {
                return;
            }
            if (player.level().getBlockEntity(pkt.pos()) instanceof SingleMachineBlockEntity machine) {
                if (pkt.controller()) {
                    machine.openController(player);
                } else {
                    machine.openSlots(player);
                }
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
