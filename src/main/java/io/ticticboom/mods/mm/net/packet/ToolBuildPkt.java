package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Right-click with the Structure Builder; the server resolves the target itself, clients can't supply distant coordinates. */
public record ToolBuildPkt(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ToolBuildPkt> TYPE = new Type<>(Ref.id("tool_build"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolBuildPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeEnum(pkt.hand), buf -> new ToolBuildPkt(buf.readEnum(InteractionHand.class)));

    public static void handle(ToolBuildPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator() || player.isShiftKeyDown()
                    || player.containerMenu != player.inventoryMenu) return;
            var stack = player.getItemInHand(packet.hand);
            if (stack.getItem() instanceof MultiblockToolItem) MultiblockToolItem.buildTarget(player, stack);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
