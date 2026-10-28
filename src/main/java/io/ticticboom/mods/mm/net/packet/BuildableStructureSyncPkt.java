package io.ticticboom.mods.mm.net.packet;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;

import java.util.ArrayList;
import java.util.List;

public record BuildableStructureSyncPkt(int batch, int index, int total, List<BuildableStructure> structures) implements CustomPacketPayload {
    public static final Type<BuildableStructureSyncPkt> TYPE = new Type<>(Ref.id("buildable_structure_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuildableStructureSyncPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), BuildableStructureSyncPkt::decode);


    public static void encode(BuildableStructureSyncPkt pkt, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(pkt.batch);
        buf.writeVarInt(pkt.index);
        buf.writeVarInt(pkt.total);
        buf.writeVarInt(pkt.structures.size());
        for (BuildableStructure structure : pkt.structures) {
            BuildableStructureSync.write(buf, structure);
        }
    }

    public static BuildableStructureSyncPkt decode(RegistryFriendlyByteBuf buf) {
        int batch = buf.readVarInt();
        int index = buf.readVarInt();
        int total = buf.readVarInt();
        int count = buf.readVarInt();
        List<BuildableStructure> structures = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) {
            BuildableStructure structure = BuildableStructureSync.read(buf);
            if (structure != null) {
                structures.add(structure);
            }
        }
        return new BuildableStructureSyncPkt(batch, index, total, List.copyOf(structures));
    }

    public static void handle(BuildableStructureSyncPkt pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BuildableStructureSync.receive(pkt));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
