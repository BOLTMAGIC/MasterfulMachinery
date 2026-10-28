package io.ticticboom.mods.mm.cap;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import net.neoforged.neoforge.capabilities.ItemCapability;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * On Forge a block entity answered capability queries itself by overriding getCapability, and MM's
 * port block entities delegated that to their IPortStorage.
 * <p>
 * NeoForge inverts this: capabilities are registered up front against a BlockEntityType and the
 * query is answered by a lookup function. The per-block-entity overrides are therefore gone, and
 * the delegation to IPortStorage happens here instead, once for every port type MM registered.
 * <p>
 * The capability objects are no longer created by the mod either. NeoForge ships the item, fluid
 * and energy block capabilities, so these constants are aliases kept only so the rest of the
 * codebase can carry on referring to MMCapabilities.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class MMCapabilities {
    public static final BlockCapability<IItemHandler, Direction> ITEM = Capabilities.ItemHandler.BLOCK;
    public static final BlockCapability<IFluidHandler, Direction> FLUID = Capabilities.FluidHandler.BLOCK;
    public static final BlockCapability<IEnergyStorage, Direction> ENERGY = Capabilities.EnergyStorage.BLOCK;
    public static final ItemCapability<IEnergyStorage, Void> ENERGY_ITEM = Capabilities.EnergyStorage.ITEM;

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var capabilities = new ArrayList<BlockCapability<?, Direction>>(List.of(ITEM, FLUID, ENERGY));
        capabilities.addAll(compatCapabilities());

        // Port block entity types are built from data at load time rather than declared
        // statically, so this walks whatever MMPortRegistry ended up with.
        for (var holder : MMPortRegistry.PORTS) {
            var beType = holder.getBe().get();
            if (beType == null) {
                continue;
            }
            for (var capability : capabilities) {
                registerPortCapability(event, beType, capability);
            }
            if (ModList.get().isLoaded("ae2")) {
                Ae2Capabilities.registerNodeHost(event, beType);
                Ae2Capabilities.registerItemPortStorage(event, beType);
            }
        }
        for (var holder : MMControllerRegistry.CONTROLLERS) {
            if (Ref.Controller.SINGLE.equals(holder.getRegistryId())) {
                @SuppressWarnings("unchecked")
                var single = (BlockEntityType<SingleMachineBlockEntity>) holder.getBe().get();
                event.registerBlockEntity(ITEM, single, (be, side) -> be.itemHandler());
                event.registerBlockEntity(FLUID, single, (be, side) -> be.fluidHandler());
                event.registerBlockEntity(ENERGY, single, (be, side) -> be.energyHandler());
                // [26.1-pending] Mekanism has no 26.1.2 release yet
                // if (ModList.get().isLoaded("mekanism")) {
                // MekCapabilities.registerSingle(event, single);
                // }
            }
        }
        event.registerItem(ENERGY_ITEM, (stack, context) -> ToolEnergy.of(stack), MMRegisters.MULTIBLOCK_TOOL.get());
        var gateway = MMRegisters.INPUT_GATEWAY_BE.get();
        event.registerBlockEntity(ITEM, gateway, (be, side) -> be.itemHandler());
        event.registerBlockEntity(FLUID, gateway, (be, side) -> be.fluidHandler());
        event.registerBlockEntity(ENERGY, gateway, (be, side) -> be.energyHandler());
        // [26.1-pending] Mekanism has no 26.1.2 release yet
        // if (ModList.get().isLoaded("mekanism")) {
        // MekCapabilities.registerGateway(event, gateway);
        // }
        if (ModList.get().isLoaded("ae2")) {
            Ae2Capabilities.registerPatternPort(event, MMRegisters.PATTERN_PORT_BE.get());
        }
    }

    /**
     * The compat ports answer capabilities their own mod owns rather than one of NeoForge's, and a
     * capability nothing registers is a capability nothing can ever query: the Mekanism and
     * PneumaticCraft ports would work inside a machine but be invisible to every pipe.
     * <p>
     * The constants live in separate classes, reached only from inside these branches, so the
     * foreign types are never resolved when the mod is absent.
     */
    private static List<BlockCapability<?, Direction>> compatCapabilities() {
        var result = new ArrayList<BlockCapability<?, Direction>>();
        // [26.1-pending] Mekanism has no 26.1.2 release yet
        // if (ModList.get().isLoaded("mekanism")) {
        // result.add(MekCapabilities.CHEMICAL);
        // result.add(MekCapabilities.HEAT);
        // }
        // [26.1-pending] PneumaticCraft has no 26.1.2 release yet
        // if (ModList.get().isLoaded("pneumaticcraft")) {
        // result.add(PncCapabilities.AIR_HANDLER_MACHINE);
        // }
        if (ModList.get().isLoaded("replication")) {
            result.add(ReplicationCapabilities.MATTER_HANDLER);
        }
        // [26.1-pending] Ars Nouveau has no 26.1.2 release yet
        // if (ModList.get().isLoaded("ars_nouveau")) {
        // result.add(ArsCapabilities.SOURCE);
        // }
        // [26.1-pending] ProjectE has no 26.1.2 release yet
        // if (ModList.get().isLoaded("projecte")) {
        // result.add(ProjectECapabilities.EMC_STORAGE);
        // }
        return result;
    }

    private static <T> void registerPortCapability(RegisterCapabilitiesEvent event,
                                                   BlockEntityType<?> beType,
                                                   BlockCapability<T, Direction> capability) {
        @SuppressWarnings("unchecked")
        var typed = (BlockEntityType<BlockEntity>) beType;
        event.registerBlockEntity(capability, typed, (be, side) -> {
            if (be instanceof IPortBlockEntity port) {
                var storage = port.getStorage();
                return storage == null ? null : storage.getCapability(capability);
            }
            return null;
        });
    }
}
