package io.ticticboom.mods.mm.net.packet;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public record MMConfigSyncPkt(Map<String, String> values) implements CustomPacketPayload {
    public static final Type<MMConfigSyncPkt> TYPE = new Type<>(Ref.id("mm_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MMConfigSyncPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), MMConfigSyncPkt::decode);

    private static Consumer<Map<String, String>> clientHandler = values -> {};

    public static void setClientHandler(Consumer<Map<String, String>> handler) { clientHandler = handler; }

    public static void encode(MMConfigSyncPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.values.size());
        packet.values.forEach((key, value) -> {
            buf.writeUtf(key, 80);
            buf.writeUtf(value, 32);
        });
    }

    public static MMConfigSyncPkt decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid MM config option count");
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) values.put(buf.readUtf(80), buf.readUtf(32));
        return new MMConfigSyncPkt(Map.copyOf(values));
    }

    public static void handle(MMConfigSyncPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> clientHandler.accept(packet.values));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
