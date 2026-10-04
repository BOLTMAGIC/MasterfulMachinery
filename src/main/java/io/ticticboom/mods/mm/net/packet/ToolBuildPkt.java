package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server resolves the target itself; clients cannot supply arbitrary distant coordinates. */
public record ToolBuildPkt(InteractionHand hand) {
    public static void encode(ToolBuildPkt packet, FriendlyByteBuf buf) { buf.writeEnum(packet.hand); }
    public static ToolBuildPkt decode(FriendlyByteBuf buf) { return new ToolBuildPkt(buf.readEnum(InteractionHand.class)); }

    public static void handle(ToolBuildPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || player.isShiftKeyDown()
                    || player.containerMenu != player.inventoryMenu) return;
            var stack = player.getItemInHand(packet.hand);
            if (stack.getItem() instanceof MultiblockToolItem) MultiblockToolItem.buildTarget(player, stack);
        });
        ctx.get().setPacketHandled(true);
    }
}
