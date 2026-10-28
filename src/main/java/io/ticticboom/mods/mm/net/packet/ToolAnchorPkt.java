package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolTarget;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** G pressed with the Structure Builder: anchors the placement at the aimed block, or releases the anchor. */
public record ToolAnchorPkt(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ToolAnchorPkt> TYPE = new Type<>(Ref.id("tool_anchor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolAnchorPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeEnum(pkt.hand), buf -> new ToolAnchorPkt(buf.readEnum(InteractionHand.class)));

    public static void handle(ToolAnchorPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                    || player.containerMenu != player.inventoryMenu) return;
            var stack = player.getItemInHand(packet.hand);
            if (!(stack.getItem() instanceof MultiblockToolItem)) return;
            if (ToolData.anchor(stack) != null) {
                ToolData.setAnchor(stack, null);
                player.sendOverlayMessage(Component.translatable("message.mm.tool.anchor.released"));
                return;
            }
            if (!ToolTarget.hasSelection(stack)) {
                player.sendOverlayMessage(Component.translatable("message.mm.tool.no_structure"));
                return;
            }
            ToolTarget target = ToolTarget.trace(player, MMConfigSetup.COMMON.toolBuildRange.get());
            if (target == null) {
                player.sendOverlayMessage(Component.translatable("message.mm.tool.target_missing"));
                return;
            }
            ToolData.setAnchor(stack, new ToolData.Anchor(player.level().dimension().identifier(), target.pos(), target.face(), target.facing()));
            player.sendOverlayMessage(Component.translatable("message.mm.tool.anchor.set", target.pos().toShortString()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
