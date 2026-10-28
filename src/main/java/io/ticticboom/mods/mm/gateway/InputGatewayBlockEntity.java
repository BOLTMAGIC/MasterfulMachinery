package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.MachineLinks;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InputGatewayBlockEntity extends BlockEntity {
    private static final int REFRESH_TICKS = 20;
    private static final int RELINK_EVERY = 10;

    private final IItemHandler itemHandler = new GatewayItemHandler(this::inputs);
    private final IFluidHandler fluidHandler = new GatewayFluidHandler(this::inputs);
    private final IEnergyStorage energyHandler = new GatewayEnergyHandler(this::inputs);
    private Object chemicalHandler;

    private List<IPortStorage> cachedInputs = List.of();
    private long refreshAt = Long.MIN_VALUE;
    private int refreshCount = 0;
    @Nullable
    private BlockPos controllerPos;

    public InputGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.INPUT_GATEWAY_BE.get(), pos, state);
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    public IEnergyStorage energyHandler() {
        return energyHandler;
    }

    public Object chemicalHandler() {
        if (chemicalHandler == null) {
            // [26.1-pending] Mekanism has no 26.1.2 release yet: no chemical handler (only MekCapabilities asks for it)
            // chemicalHandler = new GatewayChemicalHandler(this::inputs);
            return null;
        }
        return chemicalHandler;
    }

    List<IPortStorage> inputs() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        long now = serverLevel.getGameTime();
        if (now < refreshAt) {
            return cachedInputs;
        }
        refreshAt = now + REFRESH_TICKS;
        if (++refreshCount % RELINK_EVERY == 0) {
            controllerPos = null;
        }
        MachineControllerBlockEntity controller = linkedController(serverLevel);
        if (controller == null) {
            controller = searchController(serverLevel);
            controllerPos = controller == null ? null : controller.getBlockPos();
        }
        var storages = controller == null ? null : controller.getPortStorages();
        cachedInputs = storages == null ? List.of() : storages.inputStorages();
        return cachedInputs;
    }

    @Nullable
    private MachineControllerBlockEntity linkedController(ServerLevel serverLevel) {
        if (controllerPos != null && serverLevel.isLoaded(controllerPos)
                && serverLevel.getBlockEntity(controllerPos) instanceof MachineControllerBlockEntity controller
                && controller.getPortStorages() != null) {
            return controller;
        }
        return null;
    }

    @Nullable
    private MachineControllerBlockEntity searchController(ServerLevel serverLevel) {
        return MachineLinks.find(serverLevel, getBlockPos());
    }
}
