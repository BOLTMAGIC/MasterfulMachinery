package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.tool.ToolDismantles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client held V on a machine while holding the multiblock tool -> server: dismantle it now. */
public record ToolDismantlePkt(BlockPos pos) {
    /** How far the aimed-at block may be from the player's eyes. */
    private static final double MAX_DISTANCE = 8;

    public static void encode(ToolDismantlePkt pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static ToolDismantlePkt decode(FriendlyByteBuf buf) {
        return new ToolDismantlePkt(buf.readBlockPos());
    }

    public static void handle(ToolDismantlePkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            ItemStack tool = ToolRotatePkt.heldTool(sender);
            if (tool == null || !sender.level().isLoaded(pkt.pos)
                    || sender.getEyePosition().distanceToSqr(pkt.pos.getCenter()) > MAX_DISTANCE * MAX_DISTANCE) {
                return;
            }
            // target, access, build rights and energy are checked like a Shift+right-click
            ToolDismantles.start(sender, tool, pkt.pos);
        });
        ctx.get().setPacketHandled(true);
    }
}
