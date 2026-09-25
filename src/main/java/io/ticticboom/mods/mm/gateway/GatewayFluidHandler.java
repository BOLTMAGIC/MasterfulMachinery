package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Looks like one always-empty tank; filled fluid goes on to the machine's fluid input ports,
 * filling them in order. Nothing can be drained.
 */
public class GatewayFluidHandler implements IFluidHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayFluidHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (IPortStorage storage : inputs.get()) {
            if (filled >= resource.getAmount()) {
                break;
            }
            var handler = storage.getCapability(MMCapabilities.FLUID).resolve();
            if (handler.isPresent()) {
                filled += handler.get().fill(new FluidStack(resource, resource.getAmount() - filled), action);
            }
        }
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
