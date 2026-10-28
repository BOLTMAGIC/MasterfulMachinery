package io.ticticboom.mods.mm.cap;

import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.gateway.InputGatewayBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Mekanism 1.21.1 merged the gas, slurry, pigment and infusion capabilities into a single chemical
 * capability, so the four separate constants collapse to one. CHEMICAL is kept as the name; the
 * old per-kind fields are gone because there is nothing left to distinguish.
 * <p>
 * Kept in its own class so the Mekanism types it names are only resolved when the mod is actually
 * installed; MMCapabilities touches it from behind a ModList check.
 */
public class MekCapabilities {
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL = Capabilities.CHEMICAL.block();
    public static final BlockCapability<IHeatHandler, Direction> HEAT = Capabilities.HEAT;

    public static void registerSingle(RegisterCapabilitiesEvent event, BlockEntityType<SingleMachineBlockEntity> single) {
        event.registerBlockEntity(CHEMICAL, single, (be, side) -> (IChemicalHandler) be.chemicalHandler());
    }

    public static void registerGateway(RegisterCapabilitiesEvent event, BlockEntityType<InputGatewayBlockEntity> gateway) {
        event.registerBlockEntity(CHEMICAL, gateway, (be, side) -> (IChemicalHandler) be.chemicalHandler());
    }
}
