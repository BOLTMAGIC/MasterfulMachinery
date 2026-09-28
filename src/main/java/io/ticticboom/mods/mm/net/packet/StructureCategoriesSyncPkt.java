package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Server -> client: the complete gallery category configuration. */
public record StructureCategoriesSyncPkt(List<String> categories, Map<String, String> assignments) {
    public StructureCategoriesSyncPkt(StructureCategories.Snapshot snapshot) {
        this(snapshot.categories(), snapshot.assignments());
    }

    public static void encode(StructureCategoriesSyncPkt packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.categories.size());
        for (String name : packet.categories) buf.writeUtf(name, 32);
        buf.writeVarInt(packet.assignments.size());
        packet.assignments.forEach((key, category) -> {
            buf.writeUtf(key, 256);
            buf.writeUtf(category, 32);
        });
    }

    public static StructureCategoriesSyncPkt decode(FriendlyByteBuf buf) {
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

    public static void handle(StructureCategoriesSyncPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> StructureCategories.receive(packet.categories, packet.assignments));
        ctx.get().setPacketHandled(true);
    }
}
