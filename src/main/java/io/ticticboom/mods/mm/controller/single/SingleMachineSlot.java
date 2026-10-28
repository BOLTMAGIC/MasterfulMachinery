package io.ticticboom.mods.mm.controller.single;

import io.ticticboom.mods.mm.port.IPortStorageFactory;
import net.minecraft.resources.Identifier;

public record SingleMachineSlot(String id, Identifier type, boolean input, IPortStorageFactory factory) {
}
