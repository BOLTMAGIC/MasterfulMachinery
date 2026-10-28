package io.ticticboom.mods.mm.port.fluid;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class WrappedFluidPortHandler implements IFluidHandler, TransferSnapshot {
    private final FluidPortHandler handler;

    public WrappedFluidPortHandler(FluidPortHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object createSnapshot() {
        return handler.createSnapshot();
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        handler.restoreSnapshot(snapshot);
    }

    @Override
    public int getTanks() {
        return handler.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return handler.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return handler.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return handler.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            int tankCapacity = handler.getTankCapacity(i);
            if (stack.getAmount() >= tankCapacity || !handler.isFluidValid(i, resource)) {
                continue;
            }
            int filled = Math.min(tankCapacity - stack.getAmount(), resource.getAmount());
            if (action.execute()) {
                handler.setFluidInTank(i, new FluidStack(resource.getFluid(), stack.getAmount() + filled));
            }
            return filled;
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int i = 0; i < handler.getTanks(); i++) {
            var taken = handler.innerDrain(i, resource.getFluid(), resource.getAmount(), action.simulate());
            if (taken.getAmount() != 0) {
                if (action.execute()) {
                    handler.getChanged().call();
                }
                return taken;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (stack.isEmpty()) {
                continue;
            }
            var taken = handler.innerDrain(i, stack.getFluid(), maxDrain, action.simulate());
            if (taken.getAmount() != 0) {
                if (action.execute()) {
                    handler.getChanged().call();
                }
                return taken;
            }
        }
        return FluidStack.EMPTY;
    }
}
