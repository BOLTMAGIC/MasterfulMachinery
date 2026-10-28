package io.ticticboom.mods.mm.cap;

import com.buuz135.replication.ReplicationRegistry;
import com.buuz135.replication.api.matter_fluid.IMatterHandler;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Kept in its own class so the Replication types it names are only resolved when the mod is
 * actually installed; MMCapabilities touches it from behind a ModList check.
 */
public class ReplicationCapabilities {
    public static final BlockCapability<IMatterHandler, Direction> MATTER_HANDLER =
            ReplicationRegistry.Capabilities.MATTER_HANDLER;
}
