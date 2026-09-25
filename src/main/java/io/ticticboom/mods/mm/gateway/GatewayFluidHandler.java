package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shows the tanks of the machine's fluid input ports (read only), followed by one always-empty tank to fill;
 * filled fluid goes on to the input ports, filling them in order. Nothing can be drained.
 * Showing the ports' contents lets an AE2 Pattern Provider in blocking mode wait until the machine has used its inputs.
 */
public class GatewayFluidHandler implements IFluidHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayFluidHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IFluidHandler> handlers() {
        var result = new ArrayList<IFluidHandler>();
        for (IPortStorage storage : inputs.get()) {
            storage.getCapability(MMCapabilities.FLUID).resolve().ifPresent(result::add);
        }
        return result;
    }

    @Override
    public int getTanks() {
        int tanks = 1;
        for (IFluidHandler handler : handlers()) {
            tanks += handler.getTanks();
        }
        return tanks;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        for (IFluidHandler handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getFluidInTank(tank);
            }
            tank -= handler.getTanks();
        }
        // the trailing entry tank
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getTankCapacity(tank);
            }
            tank -= handler.getTanks();
        }
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
