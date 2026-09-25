package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.compat.ftbteams.FtbTeamsCompat;
import io.ticticboom.mods.mm.config.MMConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

public final class Permissions {
    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    private Permissions() {
    }

    /**
     * The owner, their FTB team and (if enabled) operators may use a linked machine.
     */
    public static boolean canAccess(Player player, UUID owner) {
        if (player.getUUID().equals(owner)) {
            return true;
        }
        if (MMConfig.NETWORK_LINK_OP_BYPASS && player.hasPermissions(2)) {
            return true;
        }
        return FTB_TEAMS && FtbTeamsCompat.sameTeam(player.getUUID(), owner);
    }
}
