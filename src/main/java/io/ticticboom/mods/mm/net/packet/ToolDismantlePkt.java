package io.ticticboom.mods.mm.net.packet;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.tool.ToolDismantles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;


public record ToolDismantlePkt(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToolDismantlePkt> TYPE = new Type<>(Ref.id("tool_dismantle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolDismantlePkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ToolDismantlePkt::decode);

    private static final double MAX_DISTANCE = 8;

    public static void encode(ToolDismantlePkt pkt, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static ToolDismantlePkt decode(RegistryFriendlyByteBuf buf) {
        return new ToolDismantlePkt(buf.readBlockPos());
    }

    public static void handle(ToolDismantlePkt pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = (ctx.player() instanceof ServerPlayer player ? player : null);
            if (sender == null) {
                return;
            }
            ItemStack tool = ToolRotatePkt.heldTool(sender);
            if (tool == null || !sender.level().isLoaded(pkt.pos)
                    || sender.getEyePosition().distanceToSqr(pkt.pos.getCenter()) > MAX_DISTANCE * MAX_DISTANCE) {
                return;
            }
            ToolDismantles.start(sender, tool, pkt.pos);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
