package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleRedstoneModePkt {
    public BlockPos pos;
    public int modeOrdinal;

    public ToggleRedstoneModePkt() {}

    public ToggleRedstoneModePkt(BlockPos pos, int modeOrdinal) {
        this.pos = pos;
        this.modeOrdinal = modeOrdinal;
    }

    public static void encode(ToggleRedstoneModePkt pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.modeOrdinal);
    }

    public static ToggleRedstoneModePkt decode(FriendlyByteBuf buf) {
        ToggleRedstoneModePkt pkt = new ToggleRedstoneModePkt();
        pkt.pos = buf.readBlockPos();
        pkt.modeOrdinal = buf.readVarInt();
        return pkt;
    }

    public static void handle(ToggleRedstoneModePkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            Level level = sender.level();
            if (!level.isLoaded(pkt.pos)) return;
            var be = level.getBlockEntity(pkt.pos);
            if (be instanceof MachineControllerBlockEntity mbe) {
                // simple permission check: player must be close enough
                if (sender.distanceToSqr(pkt.pos.getX()+0.5, pkt.pos.getY()+0.5, pkt.pos.getZ()+0.5) > 64*64) return;
                mbe.setRedstoneModeOrdinal(pkt.modeOrdinal);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}



