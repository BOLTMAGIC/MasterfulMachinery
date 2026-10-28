package io.ticticboom.mods.mm.cap;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Holds the one PneumaticCraft capability MM's air ports answer. Kept in its own class so the
 * PneumaticCraft types it names are only resolved when the mod is actually installed; MMCapabilities
 * touches it from behind a ModList check.
 */
public class PncCapabilities {
    public static final BlockCapability<IAirHandlerMachine, Direction> AIR_HANDLER_MACHINE = PNCCapabilities.AIR_HANDLER_MACHINE;
}
