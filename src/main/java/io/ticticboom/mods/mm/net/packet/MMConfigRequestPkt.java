package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.config.MMConfigOptions;
import io.ticticboom.mods.mm.net.MMNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Requests the server's live MM settings for the in-game config page. */
public record MMConfigRequestPkt() {
    public static void encode(MMConfigRequestPkt packet, FriendlyByteBuf buf) {}
    public static MMConfigRequestPkt decode(FriendlyByteBuf buf) { return new MMConfigRequestPkt(); }

    public static void handle(MMConfigRequestPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                MMNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                        new MMConfigSyncPkt(MMConfigOptions.serverSnapshot()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
