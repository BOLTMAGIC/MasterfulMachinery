package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InputGatewayBlockEntity extends BlockEntity {
    /** How long the found input ports are trusted before looking again, in ticks. */
    private static final int REFRESH_TICKS = 20;

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new GatewayItemHandler(this::inputs));
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> new GatewayFluidHandler(this::inputs));

    /** Every this many refreshes the machine is looked up from scratch. */
    private static final int RELINK_EVERY = 10;

    private List<IPortStorage> cachedInputs = List.of();
    private long refreshAt = Long.MIN_VALUE;
    private int refreshCount = 0;
    @Nullable
    private BlockPos controllerPos;

    public InputGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.INPUT_GATEWAY_BE.get(), pos, state);
    }

    /**
     * @return the input port storages of the machine this gateway feeds, empty if there is none.
     * Looked up again at most once a second, as pipes insert every tick.
     */
    List<IPortStorage> inputs() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        long now = serverLevel.getGameTime();
        if (now < refreshAt) {
            return cachedInputs;
        }
        refreshAt = now + REFRESH_TICKS;
        // re-check which machine we belong to now and then; in between only ask the known controller
        if (++refreshCount % RELINK_EVERY == 0) {
            controllerPos = null;
        }
        cachedInputs = findInputs(serverLevel);
        return cachedInputs;
    }

    private List<IPortStorage> findInputs(ServerLevel serverLevel) {
        MachineControllerBlockEntity controller = linkedController(serverLevel);
        if (controller == null) {
            controller = searchController(serverLevel);
            controllerPos = controller == null ? null : controller.getBlockPos();
        }
        if (controller == null) {
            return List.of();
        }
        RecipeStorages storages = controller.getCachedPortStorages();
        if (storages == null) {
            storages = controller.getStructure().getStorages(serverLevel, controller.getBlockPos());
        }
        return storages == null ? List.of() : new ArrayList<>(storages.inputStorages());
    }

    @Nullable
    private MachineControllerBlockEntity linkedController(ServerLevel serverLevel) {
        if (controllerPos != null && serverLevel.getExistingBlockEntity(controllerPos) instanceof MachineControllerBlockEntity controller
                && controller.getStructure() != null) {
            return controller;
        }
        controllerPos = null;
        return null;
    }

    /**
     * The gateway feeds a machine it is built into (standing in for a casing or glass block), or one
     * whose input port it touches.
     */
    @Nullable
    private MachineControllerBlockEntity searchController(ServerLevel serverLevel) {
        BlockPos self = getBlockPos();
        var builtInto = PortSides.findController(serverLevel, self, controller -> controller.getStructure() != null
                && controller.getStructure().getPositions(serverLevel, controller.getBlockPos()).contains(self));
        if (builtInto != null) {
            return builtInto;
        }
        for (Direction dir : Direction.values()) {
            BlockPos portPos = self.relative(dir);
            if (serverLevel.getExistingBlockEntity(portPos) instanceof IPortBlockEntity port && port.isInput()) {
                var controller = PortSides.findController(serverLevel, portPos, port.getStorage());
                if (controller != null && controller.getStructure() != null) {
                    return controller;
                }
            }
        }
        return null;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MMCapabilities.ITEM) {
            return itemHandler.cast();
        }
        if (cap == MMCapabilities.FLUID) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        fluidHandler.invalidate();
    }
}
