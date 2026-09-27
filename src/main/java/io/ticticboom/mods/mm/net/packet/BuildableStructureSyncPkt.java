package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> client: chunk {@code index} of {@code total} of the builder structure list sync number {@code batch}.
 * See {@link BuildableStructureSync}.
 */
public record BuildableStructureSyncPkt(int batch, int index, int total, List<BuildableStructure> structures) {

    public static void encode(BuildableStructureSyncPkt pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.batch);
        buf.writeVarInt(pkt.index);
        buf.writeVarInt(pkt.total);
        buf.writeVarInt(pkt.structures.size());
        for (BuildableStructure structure : pkt.structures) {
            BuildableStructureSync.write(buf, structure);
        }
    }

    public static BuildableStructureSyncPkt decode(FriendlyByteBuf buf) {
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

    public static void handle(BuildableStructureSyncPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BuildableStructureSync.receive(pkt));
        ctx.get().setPacketHandled(true);
    }
}
