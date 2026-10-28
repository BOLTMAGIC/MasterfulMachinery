package io.ticticboom.mods.mm.compat.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

public final class FtbTeamsCompat {

    private FtbTeamsCompat() {
    }

    public static boolean sameTeam(UUID a, UUID b) {
        var api = FTBTeamsAPI.api();
        return api.isManagerLoaded() && api.getManager().arePlayersInSameTeam(a, b);
    }
}
