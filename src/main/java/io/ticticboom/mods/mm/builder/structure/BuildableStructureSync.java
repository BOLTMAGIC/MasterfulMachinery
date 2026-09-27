package io.ticticboom.mods.mm.builder.structure;

import io.netty.buffer.Unpooled;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.BuildableStructureSyncPkt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends {@link BuildableStructureRegistry#SERVER} to clients (on login and after /reload) and rebuilds it as
 * {@link BuildableStructureRegistry#CLIENT}. The list is split into packets that each stay under
 * {@link #MAX_PACKET_BYTES}; each structure is written as a palette of block state ids plus (x, y, z, palette index)
 * per block. A sync is one batch; the client swaps its registry only when every chunk of the batch has arrived.
 */
public final class BuildableStructureSync {
    public static final int MAX_PACKET_BYTES = 512 * 1024;
    /** Room for the packet's own header, the channel name and the message discriminator. */
    private static final int HEADER_BYTES = 64;
    /** A sane upper bound on chunks per batch, so a bad packet can't make the client allocate a huge array. */
    private static final int MAX_CHUNKS = 4096;

    private static final AtomicInteger BATCHES = new AtomicInteger();
    private static final Assembler CLIENT_ASSEMBLER = new Assembler();

    private BuildableStructureSync() {
    }

    public static void send(PacketDistributor.PacketTarget target) {
        for (BuildableStructureSyncPkt pkt : split(BuildableStructureRegistry.SERVER.all(), BATCHES.incrementAndGet(), MAX_PACKET_BYTES)) {
            MMNetwork.INSTANCE.send(target, pkt);
        }
    }

    /** Client thread: one chunk arrived. */
    public static void receive(BuildableStructureSyncPkt pkt) {
        List<BuildableStructure> complete = CLIENT_ASSEMBLER.accept(pkt);
        if (complete != null) {
            BuildableStructureRegistry.CLIENT.replace(complete);
        }
    }

    /** Client logout: forget the server's structures and any half-received batch. */
    public static void resetClient() {
        CLIENT_ASSEMBLER.reset();
        BuildableStructureRegistry.CLIENT.clear();
    }

    /**
     * Packs the structures into as few packets as fit {@code maxBytes} each; always at least one packet, so an empty
     * list still clears the client. A single structure too large for a packet is left out with a warning.
     */
    public static List<BuildableStructureSyncPkt> split(Collection<BuildableStructure> structures, int batch, int maxBytes) {
        int budget = maxBytes - HEADER_BYTES;
        List<List<BuildableStructure>> chunks = new ArrayList<>();
        List<BuildableStructure> current = new ArrayList<>();
        int currentBytes = 0;
        for (BuildableStructure structure : structures) {
            int size = encodedSize(structure);
            if (size > budget) {
                Ref.LOG.warn("Builder structure {} is too large to sync ({} bytes), clients won't see it", structure.id(), size);
                continue;
            }
            if (currentBytes + size > budget && !current.isEmpty()) {
                chunks.add(current);
                current = new ArrayList<>();
                currentBytes = 0;
            }
            current.add(structure);
            currentBytes += size;
        }
        if (!current.isEmpty() || chunks.isEmpty()) {
            chunks.add(current);
        }
        List<BuildableStructureSyncPkt> packets = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            packets.add(new BuildableStructureSyncPkt(batch, i, chunks.size(), List.copyOf(chunks.get(i))));
        }
        return packets;
    }

    public static void write(FriendlyByteBuf buf, BuildableStructure structure) {
        buf.writeResourceLocation(structure.id());
        buf.writeVarInt(structure.size().getX());
        buf.writeVarInt(structure.size().getY());
        buf.writeVarInt(structure.size().getZ());
        Map<BlockState, Integer> palette = new IdentityHashMap<>();
        List<BlockState> order = new ArrayList<>();
        for (BuildableStructure.Placement placement : structure.blocks()) {
            if (palette.putIfAbsent(placement.state(), order.size()) == null) {
                order.add(placement.state());
            }
        }
        buf.writeVarInt(order.size());
        for (BlockState state : order) {
            buf.writeVarInt(Block.getId(state));
        }
        buf.writeVarInt(structure.blocks().size());
        for (BuildableStructure.Placement placement : structure.blocks()) {
            buf.writeVarInt(placement.pos().getX());
            buf.writeVarInt(placement.pos().getY());
            buf.writeVarInt(placement.pos().getZ());
            buf.writeVarInt(palette.get(placement.state()));
        }
    }

    /** Reads one structure; null (after consuming its bytes) if it names a block state this client doesn't know. */
    @Nullable
    public static BuildableStructure read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Vec3i size = new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        int paletteSize = buf.readVarInt();
        List<BlockState> palette = new ArrayList<>(Math.min(paletteSize, 4096));
        boolean known = true;
        for (int i = 0; i < paletteSize; i++) {
            BlockState state = Block.BLOCK_STATE_REGISTRY.byId(buf.readVarInt());
            known &= state != null;
            palette.add(state);
        }
        int count = buf.readVarInt();
        List<BuildableStructure.Placement> blocks = new ArrayList<>(Math.min(count, 65536));
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            int index = buf.readVarInt();
            if (index < 0 || index >= palette.size()) {
                known = false;
            } else if (known) {
                blocks.add(new BuildableStructure.Placement(pos, palette.get(index)));
            }
        }
        if (!known) {
            Ref.LOG.warn("Dropping synced builder structure {}: unknown block state", id);
            return null;
        }
        return BuildableStructure.of(id, size, blocks);
    }

    private static int encodedSize(BuildableStructure structure) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buf, structure);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }

    /** Collects the chunks of one batch; a chunk of a different batch starts over (only the newest sync matters). */
    public static final class Assembler {
        private int batch = -1;
        @Nullable
        private List<BuildableStructure>[] parts;
        private int received;

        /** @return every structure of the batch once its last chunk arrived, else null */
        @Nullable
        @SuppressWarnings("unchecked")
        public List<BuildableStructure> accept(BuildableStructureSyncPkt pkt) {
            if (pkt.total() <= 0 || pkt.total() > MAX_CHUNKS || pkt.index() < 0 || pkt.index() >= pkt.total()) {
                return null;
            }
            if (pkt.batch() != batch || parts == null || parts.length != pkt.total()) {
                batch = pkt.batch();
                parts = new List[pkt.total()];
                received = 0;
            }
            if (parts[pkt.index()] == null) {
                parts[pkt.index()] = pkt.structures();
                received++;
            }
            if (received < parts.length) {
                return null;
            }
            List<BuildableStructure> all = new ArrayList<>();
            for (List<BuildableStructure> part : parts) {
                all.addAll(part);
            }
            reset();
            return all;
        }

        public void reset() {
            batch = -1;
            parts = null;
            received = 0;
        }
    }
}
