package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Server -> client: every admin rule for which blocks an Input Gateway / Pattern Port may stand in for. */
public record HelperSlotsSyncPkt(Map<ResourceLocation, HelperSlotOverrides.Rule> rules) {
    public static void encode(HelperSlotsSyncPkt packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.rules.size());
        packet.rules.forEach((id, rule) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(HelperSlotOverrides.keysString(rule.keys()), 256);
            buf.writeVarInt(rule.maxHelpers() + 1);
        });
    }

    public static HelperSlotsSyncPkt decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid helper rule count");
        Map<ResourceLocation, HelperSlotOverrides.Rule> rules = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Set<Character> keys = new LinkedHashSet<>();
            for (char c : buf.readUtf(256).toCharArray()) keys.add(c);
            rules.put(id, new HelperSlotOverrides.Rule(keys, buf.readVarInt() - 1));
        }
        return new HelperSlotsSyncPkt(Map.copyOf(rules));
    }

    public static void handle(HelperSlotsSyncPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> HelperSlotOverrides.receive(packet.rules));
        ctx.get().setPacketHandled(true);
    }
}
