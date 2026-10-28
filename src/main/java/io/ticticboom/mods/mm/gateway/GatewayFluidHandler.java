package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayFluidHandler implements IFluidHandler, TransferSnapshot {
    private final Supplier<List<IPortStorage>> inputs;
    private final Supplier<List<IPortStorage>> outputs;

    public GatewayFluidHandler(Supplier<List<IPortStorage>> inputs) {
        this(inputs, List::of);
    }

    public GatewayFluidHandler(Supplier<List<IPortStorage>> inputs, Supplier<List<IPortStorage>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private static List<IFluidHandler> handlers(List<IPortStorage> storages) {
        var result = new ArrayList<IFluidHandler>();
        for (IPortStorage storage : storages) {
            var handler = storage.getCapability(MMCapabilities.FLUID);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    private List<IFluidHandler> all() {
        var result = handlers(inputs.get());
        result.addAll(handlers(outputs.get()));
        return result;
    }

    @Override
    public Object createSnapshot() {
        return GatewaySnapshots.create(all());
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        GatewaySnapshots.restore(snapshot);
    }

    @Override
    public int getTanks() {
        int tanks = 1;
        for (IFluidHandler handler : all()) {
            tanks += handler.getTanks();
        }
        return tanks;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        for (IFluidHandler handler : all()) {
            if (tank < handler.getTanks()) {
                return handler.getFluidInTank(tank);
            }
            tank -= handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler handler : all()) {
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
        for (IFluidHandler handler : handlers(inputs.get())) {
            if (filled >= resource.getAmount()) {
                break;
            }
            filled += handler.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
        }
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = 0;
        for (IFluidHandler handler : handlers(outputs.get())) {
            if (drained >= resource.getAmount()) {
                break;
            }
            drained += handler.drain(resource.copyWithAmount(resource.getAmount() - drained), action).getAmount();
        }
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        for (IFluidHandler handler : handlers(outputs.get())) {
            var simulated = handler.drain(maxDrain, FluidAction.SIMULATE);
            if (!simulated.isEmpty()) {
                return drain(simulated, action);
            }
        }
        return FluidStack.EMPTY;
    }
}
