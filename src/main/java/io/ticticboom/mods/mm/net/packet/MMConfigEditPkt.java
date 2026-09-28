package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.config.MMConfigOptions;
import io.ticticboom.mods.mm.net.MMNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** One server-side MM setting change, accepted only from an operator and validated against the config spec. */
public record MMConfigEditPkt(String key, String value) {
    public static void encode(MMConfigEditPkt packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.key, 80);
        buf.writeUtf(packet.value, 32);
    }

    public static MMConfigEditPkt decode(FriendlyByteBuf buf) {
        return new MMConfigEditPkt(buf.readUtf(80), buf.readUtf(32));
    }

    public static void handle(MMConfigEditPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) return;
            MMConfigOptions.Option option = MMConfigOptions.server(packet.key);
            if (option == null || !option.set(packet.value)) return;
            MMConfig.bake();
            MMNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new MMConfigSyncPkt(MMConfigOptions.serverSnapshot()));
        });
        ctx.get().setPacketHandled(true);
    }
}
