package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: change a port's auto I/O sides, toggle its lock or dump its contents.
 */
public class PortConfigPkt {
    public enum Action {
        TOGGLE_SIDE,
        TOGGLE_LOCK,
        DUMP
    }

    public BlockPos pos;
    public Action action;
    public int arg;

    public PortConfigPkt() {}

    public PortConfigPkt(BlockPos pos, Action action, int arg) {
        this.pos = pos;
        this.action = action;
        this.arg = arg;
    }

    public static void encode(PortConfigPkt pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeEnum(pkt.action);
        buf.writeVarInt(pkt.arg);
    }

    public static PortConfigPkt decode(FriendlyByteBuf buf) {
        PortConfigPkt pkt = new PortConfigPkt();
        pkt.pos = buf.readBlockPos();
        pkt.action = buf.readEnum(Action.class);
        pkt.arg = buf.readVarInt();
        return pkt;
    }

    public static void handle(PortConfigPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            // only accept changes for the port whose menu the player currently has open
            if (!(sender.containerMenu instanceof IPortMenu menu) || !sender.containerMenu.stillValid(sender)) return;
            if (!(menu.getBlockEntity() instanceof AbstractPortBlockEntity be) || !be.getBlockPos().equals(pkt.pos)) return;

            switch (pkt.action) {
                case TOGGLE_SIDE -> {
                    if (be.getAutoIO() == null || pkt.arg < 0 || pkt.arg >= Direction.values().length) return;
                    be.getAutoIO().toggleSide(Direction.from3DDataValue(pkt.arg));
                }
                case TOGGLE_LOCK -> {
                    if (!(be.getStorage() instanceof ILockablePortStorage storage)) return;
                    storage.setLocked(!storage.isLocked());
                }
                case DUMP -> {
                    if (!(be.getStorage() instanceof ILockablePortStorage storage)) return;
                    storage.dump();
                }
            }
            be.setChanged();
        });
        ctx.get().setPacketHandled(true);
    }
}
