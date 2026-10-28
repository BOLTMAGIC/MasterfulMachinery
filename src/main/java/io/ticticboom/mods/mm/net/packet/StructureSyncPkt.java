package io.ticticboom.mods.mm.net.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.ticticboom.mods.mm.Ref;

import java.util.HashMap;
import java.util.Map;

public class StructureSyncPkt implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureSyncPkt> TYPE =
            new CustomPacketPayload.Type<>(Ref.id("structure_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureSyncPkt> STREAM_CODEC =
            StreamCodec.of(StructureSyncPkt::encode, StructureSyncPkt::decode);

    public Map<Identifier, JsonElement> structures;

    public StructureSyncPkt(final Map<Identifier, StructureModel> structures) {
        this.structures = new HashMap<>();
        for (Map.Entry<Identifier, StructureModel> structure : structures.entrySet()) {
            this.structures.put(structure.getKey(), structure.getValue().getConfig());
        }
    }

    protected StructureSyncPkt() {
    }

    public static void encode(RegistryFriendlyByteBuf buf, StructureSyncPkt packet) {
        buf.writeInt(packet.structures.size());
        for (Map.Entry<Identifier, JsonElement> strcture : packet.structures.entrySet()) {
            String string = strcture.getValue().toString();
            buf.writeIdentifier(strcture.getKey());
            buf.writeUtf(string);
        }
    }

    public static StructureSyncPkt decode(RegistryFriendlyByteBuf buf) {
        StructureSyncPkt packet = new StructureSyncPkt();
        int initialCapacity = buf.readInt();
        packet.structures = new HashMap<>(initialCapacity);
        for (int i = 0; i < initialCapacity; i++) {
            Identifier key = buf.readIdentifier();
            String string = buf.readUtf();
            packet.structures.put(key, JsonParser.parseString(string));
        }
        return packet;
    }

    public static void handle(StructureSyncPkt packet, IPayloadContext context) {
        context.enqueueWork(() -> handler(packet));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(StructureSyncPkt packet) {
        StructureManager.receiveStructures(packet.structures);
        StructureManager.validateAllPieces();
    }
}