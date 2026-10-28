package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PortConfigPkt(BlockPos pos, Action action, int arg) implements CustomPacketPayload {

    public enum Action {
        TOGGLE_SIDE,
        TOGGLE_LOCK,
        DUMP
    }

    public static final Type<PortConfigPkt> TYPE = new Type<>(Ref.id("port_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortConfigPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeEnum(pkt.action);
                buf.writeVarInt(pkt.arg);
            },
            buf -> new PortConfigPkt(buf.readBlockPos(), buf.readEnum(Action.class), buf.readVarInt()));

    public static void handle(PortConfigPkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
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
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
