package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
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

    private List<IPortStorage> cachedInputs = List.of();
    private long refreshAt = Long.MIN_VALUE;

    public InputGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.INPUT_GATEWAY_BE.get(), pos, state);
    }

    /**
     * @return the input port storages of the formed multiblock whose input port touches this gateway,
     * empty if there is none. Looked up again at most once a second, as pipes insert every tick.
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
        cachedInputs = findInputs(serverLevel);
        return cachedInputs;
    }

    private List<IPortStorage> findInputs(ServerLevel serverLevel) {
        for (Direction dir : Direction.values()) {
            BlockPos portPos = getBlockPos().relative(dir);
            if (!(serverLevel.getExistingBlockEntity(portPos) instanceof IPortBlockEntity port) || !port.isInput()) {
                continue;
            }
            var controller = PortSides.findController(serverLevel, portPos, port.getStorage());
            if (controller == null || controller.getStructure() == null) {
                continue;
            }
            var storages = controller.getStructure().getStorages(serverLevel, controller.getBlockPos());
            if (storages != null) {
                return new ArrayList<>(storages.inputStorages());
            }
        }
        return List.of();
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
