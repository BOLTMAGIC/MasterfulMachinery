package io.ticticboom.mods.mm.cap;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.cap.transfer.LegacyEnergyHandler;
import io.ticticboom.mods.mm.cap.transfer.LegacyFluidResourceHandler;
import io.ticticboom.mods.mm.cap.transfer.LegacyItemResourceHandler;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.tool.ToolComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import org.jetbrains.annotations.Nullable;
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
 * Since 1.21.9 NeoForge's item, fluid and energy capabilities are its transfer API (ResourceHandler,
 * EnergyHandler) rather than IItemHandler, IFluidHandler and IEnergyStorage. MM's storages and recipe
 * code still work with the old handlers, so ITEM, FLUID and ENERGY are MM's own capabilities carrying
 * them, and every block that answers one also answers NeoForge's through an adapter. Use item(),
 * fluid() and energy() to query a block that may not be MM's.
 */
@EventBusSubscriber
public class MMCapabilities {
    public static final BlockCapability<IItemHandler, Direction> ITEM = BlockCapability.createSided(Ref.id("item_handler"), IItemHandler.class);
    public static final BlockCapability<IFluidHandler, Direction> FLUID = BlockCapability.createSided(Ref.id("fluid_handler"), IFluidHandler.class);
    public static final BlockCapability<IEnergyStorage, Direction> ENERGY = BlockCapability.createSided(Ref.id("energy_storage"), IEnergyStorage.class);

    public static @Nullable IItemHandler item(Level level, BlockPos pos, @Nullable Direction side) {
        var own = level.getCapability(ITEM, pos, side);
        if (own != null) {
            return own;
        }
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler == null ? null : IItemHandler.of(handler);
    }

    public static @Nullable IFluidHandler fluid(Level level, BlockPos pos, @Nullable Direction side) {
        var own = level.getCapability(FLUID, pos, side);
        if (own != null) {
            return own;
        }
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : IFluidHandler.of(handler);
    }

    public static @Nullable IEnergyStorage energy(Level level, BlockPos pos, @Nullable Direction side) {
        var own = level.getCapability(ENERGY, pos, side);
        if (own != null) {
            return own;
        }
        var handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : IEnergyStorage.of(handler);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var capabilities = compatCapabilities();

        // Port block entity types are built from data at load time rather than declared
        // statically, so this walks whatever MMPortRegistry ended up with.
        for (var holder : MMPortRegistry.PORTS) {
            var beType = holder.getBe().get();
            if (beType == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            var typed = (BlockEntityType<BlockEntity>) beType;
            registerHandlers(event, typed, portLookup(ITEM), portLookup(FLUID), portLookup(ENERGY));
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
                registerHandlers(event, single, (be, side) -> be.itemHandler(), (be, side) -> be.fluidHandler(), (be, side) -> be.energyHandler());
                // [26.1-pending] Mekanism has no 26.1.2 release yet
                // if (ModList.get().isLoaded("mekanism")) {
                // MekCapabilities.registerSingle(event, single);
                // }
            }
        }
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new ItemAccessEnergyHandler(access, ToolComponents.ENERGY.get(),
                MMConfig.TOOL_ENERGY_CAPACITY, MMConfig.TOOL_ENERGY_RECEIVE_RATE, MMConfig.TOOL_ENERGY_CAPACITY), MMRegisters.MULTIBLOCK_TOOL.get());
        registerHandlers(event, MMRegisters.INPUT_GATEWAY_BE.get(), (be, side) -> be.itemHandler(), (be, side) -> be.fluidHandler(),
                (be, side) -> be.energyHandler());
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

    /** Registers MM's handlers under its own capabilities and, adapted, under NeoForge's transfer capabilities. */
    private static <B extends BlockEntity> void registerHandlers(RegisterCapabilitiesEvent event, BlockEntityType<B> type,
                                                                 ICapabilityProvider<B, Direction, IItemHandler> items,
                                                                 ICapabilityProvider<B, Direction, IFluidHandler> fluids,
                                                                 ICapabilityProvider<B, Direction, IEnergyStorage> energy) {
        event.registerBlockEntity(ITEM, type, items);
        event.registerBlockEntity(FLUID, type, fluids);
        event.registerBlockEntity(ENERGY, type, energy);
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
            var handler = items.getCapability(be, side);
            return handler == null ? null : new LegacyItemResourceHandler(handler);
        });
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> {
            var handler = fluids.getCapability(be, side);
            return handler == null ? null : new LegacyFluidResourceHandler(handler);
        });
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> {
            var handler = energy.getCapability(be, side);
            return handler == null ? null : new LegacyEnergyHandler(handler);
        });
    }

    private static <T> ICapabilityProvider<BlockEntity, Direction, T> portLookup(BlockCapability<T, Direction> capability) {
        return (be, side) -> {
            if (be instanceof IPortBlockEntity port) {
                var storage = port.getStorage();
                return storage == null ? null : storage.getCapability(capability);
            }
            return null;
        };
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
