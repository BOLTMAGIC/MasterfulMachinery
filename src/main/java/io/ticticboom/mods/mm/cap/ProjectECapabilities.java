package io.ticticboom.mods.mm.cap;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class ProjectECapabilities {
    public static final BlockCapability<IEmcStorage, Direction> EMC_STORAGE = PECapabilities.EMC_STORAGE_CAPABILITY;
}
