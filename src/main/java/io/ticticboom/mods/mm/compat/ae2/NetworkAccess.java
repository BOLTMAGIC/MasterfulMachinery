package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class NetworkAccess {

    private NetworkAccess() {
    }

    /**
     * @return the storage of the linked AE2 network, or null when it can't be reached right now
     * (chunk not loaded, node removed, no power / channel)
     */
    @Nullable
    public static MEStorage storage(MinecraftServer server, LinkData.NetworkPos network) {
        ServerLevel level = server.getLevel(network.dimension());
        // never force-load a chunk to reach the network
        if (level == null || !level.isLoaded(network.pos())) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(network.pos());
        if (!(be instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = nodeOf(host, network.face());
        if (node == null || !node.isActive()) {
            return null;
        }
        return node.getGrid().getStorageService().getInventory();
    }

    /**
     * Tries the given face first, then every face (for hosts whose node isn't sided).
     */
    @Nullable
    public static IGridNode nodeOf(IInWorldGridNodeHost host, @Nullable Direction face) {
        IGridNode node = host.getGridNode(face);
        if (node != null) {
            return node;
        }
        for (Direction dir : Direction.values()) {
            node = host.getGridNode(dir);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
