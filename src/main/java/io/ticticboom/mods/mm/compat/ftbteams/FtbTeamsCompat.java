package io.ticticboom.mods.mm.compat.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

/**
 * Only loaded when FTB Teams is present.
 */
public final class FtbTeamsCompat {

    private FtbTeamsCompat() {
    }

    public static boolean sameTeam(UUID a, UUID b) {
        try {
            return FTBTeamsAPI.api().getManager().arePlayersInSameTeam(a, b);
        } catch (RuntimeException e) {
            // team manager not ready (e.g. during server shutdown)
            return false;
        }
    }
}
