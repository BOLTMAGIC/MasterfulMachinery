package io.ticticboom.mods.mm.cap;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class ArsCapabilities {
    public static final BlockCapability<ISourceCap, Direction> SOURCE = CapabilityRegistry.SOURCE_CAPABILITY;
}
