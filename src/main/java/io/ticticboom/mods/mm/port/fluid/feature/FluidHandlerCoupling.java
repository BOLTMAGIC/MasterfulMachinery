package io.ticticboom.mods.mm.port.fluid.feature;

import io.ticticboom.mods.mm.port.common.IHandlerCoupling;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidHandlerCoupling implements IHandlerCoupling {

    @Getter
    private final LazyOptional<IFluidHandler> fromHandler;
    @Getter
    @Setter
    private LazyOptional<IFluidHandler> toHandler;

    public FluidHandlerCoupling(LazyOptional<IFluidHandler> fromHandler, LazyOptional<IFluidHandler> toHandler) {
        this.fromHandler = fromHandler;
        this.toHandler = toHandler;
    }

    @Override
    public void attemptTransfer() {
        toHandler.ifPresent((handler) -> {
            fromHandler.ifPresent((from) -> {
                attemptTransfer(from, handler);
            });
        });
    }

    private void attemptTransfer(IFluidHandler from, IFluidHandler to) {
        FluidStack extracted = from.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (extracted.isEmpty()) {
            return;
        }

        var remaining = to.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        if (remaining > 0) {
            from.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
