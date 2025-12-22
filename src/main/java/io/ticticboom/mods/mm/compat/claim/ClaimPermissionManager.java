package io.ticticboom.mods.mm.compat.claim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClaimPermissionManager {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final List<IClaimPermissionProvider> providers = new ArrayList<>();

    public static void registerProvider(IClaimPermissionProvider provider) {
        Objects.requireNonNull(provider);
        providers.add(provider);
        LOGGER.info("Registered claim permission provider: {}", provider.getClass().getName());
    }

    /**
     * Returns true if the player is allowed to modify the block at pos. Uses registered providers first.
     * If none is decisive, falls back to server-side OP permission (level >= 2) and denies otherwise.
     */
    public static boolean canPlayerModify(ServerPlayer player, BlockPos pos) {
        // ask providers
        for (IClaimPermissionProvider p : providers) {
            try {
                Boolean res = p.canModify(player, pos);
                if (res != null) {
                    return res;
                }
            } catch (Throwable t) {
                LOGGER.debug("Claim provider {} threw an exception", p.getClass().getName(), t);
            }
        }

        // fallback: OP level >= 2 allowed
        try {
            if (player.hasPermissions(2)) return true;
        } catch (Throwable ignored) {
            // some server impls may behave differently
        }

        // default deny
        return false;
    }
}
