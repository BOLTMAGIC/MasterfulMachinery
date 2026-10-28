package io.ticticboom.mods.mm.compat.ae2.linker;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.entity.player.Player;
import io.ticticboom.mods.mm.networklink.Permissions;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class NetworkAccess {

    private NetworkAccess() {
    }

    @Nullable
    public static MEStorage storage(MinecraftServer server, LinkData.NetworkPos network) {
        IGrid grid = grid(server, network);
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    @Nullable
    public static IGrid grid(MinecraftServer server, LinkData.NetworkPos network) {
        ServerLevel level = server.getLevel(network.dimension());
        if (level == null || !level.isLoaded(network.pos())) {
            return null;
        }
        if (!(level.getBlockEntity(network.pos()) instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = nodeOf(host, network.face());
        if (node == null || !node.isActive()) {
            return null;
        }
        return node.getGrid();
    }

    @Nullable
    public static LinkData.NetworkPos clicked(Level level, BlockPos pos, Direction face, IInWorldGridNodeHost host) {
        return nodeOf(host, face) == null ? null : new LinkData.NetworkPos(level.dimension(), pos, face);
    }

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

    public static boolean mayUse(Player player, IGrid grid) {
        for (UUID owner : owners(grid)) {
            if (!Permissions.canAccess(player, owner)) {
                return false;
            }
        }
        return true;
    }

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
