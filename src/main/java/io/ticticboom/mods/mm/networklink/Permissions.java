package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.compat.ftbteams.FtbTeamsCompat;
import io.ticticboom.mods.mm.config.MMConfig;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.UUID;

public final class Permissions {
    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    private Permissions() {
    }

    public static boolean canAccess(Player player, UUID owner) {
        if (player.getUUID().equals(owner)) {
            return true;
        }
        if (MMConfig.NETWORK_LINK_OP_BYPASS && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        return FTB_TEAMS && FtbTeamsCompat.sameTeam(player.getUUID(), owner);
    }
}
