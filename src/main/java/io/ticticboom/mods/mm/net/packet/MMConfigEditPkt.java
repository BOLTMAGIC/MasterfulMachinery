package io.ticticboom.mods.mm.net.packet;

import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.config.MMConfigOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;


public record MMConfigEditPkt(String key, String value) implements CustomPacketPayload {
    public static final Type<MMConfigEditPkt> TYPE = new Type<>(Ref.id("mm_config_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MMConfigEditPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), MMConfigEditPkt::decode);

    public static void encode(MMConfigEditPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeUtf(packet.key, 80);
        buf.writeUtf(packet.value, 32);
    }

    public static MMConfigEditPkt decode(RegistryFriendlyByteBuf buf) {
        return new MMConfigEditPkt(buf.readUtf(80), buf.readUtf(32));
    }

    public static void handle(MMConfigEditPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.player() instanceof ServerPlayer sp ? sp : null;
            if (player == null || !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
            MMConfigOptions.Option option = MMConfigOptions.server(packet.key);
            if (option == null || !option.set(packet.value)) return;
            MMConfig.bake();
            PacketDistributor.sendToAllPlayers(new MMConfigSyncPkt(MMConfigOptions.serverSnapshot()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
