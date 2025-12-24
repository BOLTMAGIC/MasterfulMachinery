package io.ticticboom.mods.mm.compat.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Provider interface for claim/protection mods to decide whether a player may modify a position.
 * Implementations may return true (allow) or false (deny). If provider cannot determine, it may throw
 * or return null (and the manager will fall back to server-native checks).
 */
public interface IClaimPermissionProvider {
    Boolean canModify(ServerPlayer player, BlockPos pos);
}

