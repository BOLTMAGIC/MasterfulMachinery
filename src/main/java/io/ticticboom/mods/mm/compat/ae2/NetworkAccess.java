package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import appeng.blockentity.networking.ControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NetworkAccess {

    private NetworkAccess() {
    }

    /**
     * @return the storage of the linked AE2 network, or null when it can't be reached right now
     * (chunk not loaded, node removed, no power / channel)
     */
    @Nullable
    public static MEStorage storage(MinecraftServer server, LinkData.NetworkPos network) {
        IGrid grid = grid(server, network);
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    /** The linked AE2 grid, or null when it can't be reached right now (same rules as {@link #storage}). */
    @Nullable
    public static IGrid grid(MinecraftServer server, LinkData.NetworkPos network) {
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
        return node.getGrid();
    }

    /**
     * The network position to remember for a click on host's face (what the linker and the Multiblock Tool store), or
     * null when host has no grid node to reach a network through.
     */
    @Nullable
    public static LinkData.NetworkPos clicked(Level level, BlockPos pos, Direction face, IInWorldGridNodeHost host) {
        return nodeOf(host, face) == null ? null : new LinkData.NetworkPos(level.dimension(), pos, face);
    }

    /**
     * Who owns grid (AE2 15 has no security service of its own): the owners of its controllers, or of every node on a
     * grid without a controller, so a cable placed onto someone else's ad-hoc network does not make it yours. Nodes
     * without a known owner (placed by a machine or a command) are left out; empty means an ownerless grid.
     */
    public static Set<UUID> owners(IGrid grid) {
        Iterable<IGridNode> nodes = grid.getMachineNodes(ControllerBlockEntity.class);
        if (!nodes.iterator().hasNext()) {
            nodes = grid.getNodes();
        }
        Set<UUID> owners = new HashSet<>();
        for (IGridNode node : nodes) {
            UUID owner = node.getOwningPlayerProfileId();
            if (owner != null) {
                owners.add(owner);
            }
        }
        return owners;
    }

    /** Whether player may use grid: every owner allows it (self, FTB team, operator); an ownerless grid anyone may. */
    public static boolean mayUse(Player player, IGrid grid) {
        for (UUID owner : owners(grid)) {
            if (!Permissions.canAccess(player, owner)) {
                return false;
            }
        }
        return true;
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
