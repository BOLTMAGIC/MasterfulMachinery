package io.ticticboom.mods.mm.net.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Server -> client: authoritative common config values for the MM settings screen. */
public record MMConfigSyncPkt(Map<String, String> values) {
    private static Consumer<Map<String, String>> clientHandler = values -> {};

    public static void setClientHandler(Consumer<Map<String, String>> handler) { clientHandler = handler; }

    public static void encode(MMConfigSyncPkt packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.values.size());
        packet.values.forEach((key, value) -> {
            buf.writeUtf(key, 80);
            buf.writeUtf(value, 32);
        });
    }

    public static MMConfigSyncPkt decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid MM config option count");
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) values.put(buf.readUtf(80), buf.readUtf(32));
        return new MMConfigSyncPkt(Map.copyOf(values));
    }

    public static void handle(MMConfigSyncPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> clientHandler.accept(packet.values));
        ctx.get().setPacketHandled(true);
    }
}
