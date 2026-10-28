package io.ticticboom.mods.mm.net.packet;

import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfigOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;


public record MMConfigRequestPkt() implements CustomPacketPayload {
    public static final Type<MMConfigRequestPkt> TYPE = new Type<>(Ref.id("mm_config_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MMConfigRequestPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), MMConfigRequestPkt::decode);

    public static void encode(MMConfigRequestPkt packet, RegistryFriendlyByteBuf buf) {}
    public static MMConfigRequestPkt decode(RegistryFriendlyByteBuf buf) { return new MMConfigRequestPkt(); }

    public static void handle(MMConfigRequestPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.player() instanceof ServerPlayer sp ? sp : null;
            if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                PacketDistributor.sendToPlayer(player, new MMConfigSyncPkt(MMConfigOptions.serverSnapshot()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
