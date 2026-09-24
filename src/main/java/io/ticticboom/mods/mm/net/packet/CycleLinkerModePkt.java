package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.networklink.LinkerMode;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: sneak + mouse wheel changed the network linker's mode.
 */
public record CycleLinkerModePkt(int direction) {

    public static void encode(CycleLinkerModePkt pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.direction);
    }

    public static CycleLinkerModePkt decode(FriendlyByteBuf buf) {
        return new CycleLinkerModePkt(buf.readByte());
    }

    public static void handle(CycleLinkerModePkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            var stack = player.getMainHandItem();
            if (!NetworkLink.isLinker(stack)) return;
            var mode = LinkerMode.get(stack).cycle(Integer.signum(pkt.direction));
            mode.set(stack);
            player.displayClientMessage(mode.displayName(), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
