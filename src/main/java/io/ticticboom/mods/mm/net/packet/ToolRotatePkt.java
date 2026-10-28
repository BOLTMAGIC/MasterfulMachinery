package io.ticticboom.mods.mm.net.packet;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;


public record ToolRotatePkt(int delta) implements CustomPacketPayload {
    public static final Type<ToolRotatePkt> TYPE = new Type<>(Ref.id("tool_rotate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolRotatePkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ToolRotatePkt::decode);


    public static void encode(ToolRotatePkt pkt, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(pkt.delta);
    }

    public static ToolRotatePkt decode(RegistryFriendlyByteBuf buf) {
        return new ToolRotatePkt(buf.readVarInt());
    }

    public static void handle(ToolRotatePkt pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = (ctx.player() instanceof ServerPlayer player ? player : null);
            if (sender == null) {
                return;
            }
            ItemStack tool = heldTool(sender);
            if (tool != null) {
                ToolData.setExtraTurns(tool, ToolData.extraTurns(tool) + Math.floorMod(pkt.delta, 4));
            }
        });
    }

    static ItemStack heldTool(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MultiblockToolItem) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
