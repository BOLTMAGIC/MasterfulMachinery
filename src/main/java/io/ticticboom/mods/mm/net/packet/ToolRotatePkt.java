package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client Shift+scroll while holding the multiblock tool -> server: turn the build by {@code delta} quarter turns. */
public record ToolRotatePkt(int delta) {

    public static void encode(ToolRotatePkt pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.delta);
    }

    public static ToolRotatePkt decode(FriendlyByteBuf buf) {
        return new ToolRotatePkt(buf.readVarInt());
    }

    public static void handle(ToolRotatePkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            ItemStack tool = heldTool(sender);
            if (tool != null) {
                ToolData.setExtraTurns(tool, ToolData.extraTurns(tool) + Math.floorMod(pkt.delta, 4));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** The tool in the main hand, else the off hand; null when the player holds none. */
    static ItemStack heldTool(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MultiblockToolItem) {
                return stack;
            }
        }
        return null;
    }
}
