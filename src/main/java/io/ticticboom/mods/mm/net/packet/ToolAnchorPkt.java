package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToolAnchorPkt(InteractionHand hand) {
    public static void encode(ToolAnchorPkt packet, FriendlyByteBuf buf) { buf.writeEnum(packet.hand); }
    public static ToolAnchorPkt decode(FriendlyByteBuf buf) { return new ToolAnchorPkt(buf.readEnum(InteractionHand.class)); }

    public static void handle(ToolAnchorPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return;
            var stack = player.getItemInHand(packet.hand);
            if (!(stack.getItem() instanceof MultiblockToolItem)) return;
            if (ToolData.anchor(stack) != null) {
                ToolData.setAnchor(stack, null);
                player.displayClientMessage(Component.translatable("message.mm.tool.anchor.released"), true);
                return;
            }
            if (!ToolTarget.hasSelection(stack)) {
                player.displayClientMessage(Component.translatable("message.mm.tool.no_structure"), true);
                return;
            }
            ToolTarget target = ToolTarget.trace(player, MMConfigSetup.COMMON.toolBuildRange.get());
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.mm.tool.target_missing"), true);
                return;
            }
            ToolData.setAnchor(stack, new ToolData.Anchor(player.level().dimension().location(), target.pos(), target.face(), target.facing()));
            player.displayClientMessage(Component.translatable("message.mm.tool.anchor.set", target.pos().toShortString()), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
