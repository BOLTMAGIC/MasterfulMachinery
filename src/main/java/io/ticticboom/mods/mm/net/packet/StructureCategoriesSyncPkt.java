package io.ticticboom.mods.mm.net.packet;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.tool.StructureCategories;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StructureCategoriesSyncPkt(List<String> categories, Map<String, String> assignments) implements CustomPacketPayload {
    public static final Type<StructureCategoriesSyncPkt> TYPE = new Type<>(Ref.id("structure_categories_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureCategoriesSyncPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), StructureCategoriesSyncPkt::decode);

    public StructureCategoriesSyncPkt(StructureCategories.Snapshot snapshot) {
        this(snapshot.categories(), snapshot.assignments());
    }

    public static void encode(StructureCategoriesSyncPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.categories.size());
        for (String name : packet.categories) buf.writeUtf(name, 32);
        buf.writeVarInt(packet.assignments.size());
        packet.assignments.forEach((key, category) -> {
            buf.writeUtf(key, 256);
            buf.writeUtf(category, 32);
        });
    }

    public static StructureCategoriesSyncPkt decode(RegistryFriendlyByteBuf buf) {
        int names = buf.readVarInt();
        if (names < 0 || names > 64) throw new IllegalArgumentException("Invalid category count");
        List<String> categories = new ArrayList<>(names);
        for (int i = 0; i < names; i++) categories.add(buf.readUtf(32));
        int entries = buf.readVarInt();
        if (entries < 0 || entries > 4096) throw new IllegalArgumentException("Invalid assignment count");
        Map<String, String> assignments = new LinkedHashMap<>();
        for (int i = 0; i < entries; i++) assignments.put(buf.readUtf(256), buf.readUtf(32));
        return new StructureCategoriesSyncPkt(List.copyOf(categories), Map.copyOf(assignments));
    }

    public static void handle(StructureCategoriesSyncPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> StructureCategories.receive(packet.categories, packet.assignments));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
