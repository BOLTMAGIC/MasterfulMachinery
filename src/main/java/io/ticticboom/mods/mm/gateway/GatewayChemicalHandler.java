package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayChemicalHandler implements IChemicalHandler {
    private final Supplier<List<IPortStorage>> inputs;
    private final Supplier<List<IPortStorage>> outputs;

    public GatewayChemicalHandler(Supplier<List<IPortStorage>> inputs) {
        this(inputs, List::of);
    }

    public GatewayChemicalHandler(Supplier<List<IPortStorage>> inputs, Supplier<List<IPortStorage>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private static List<IChemicalHandler> handlers(List<IPortStorage> storages) {
        var result = new ArrayList<IChemicalHandler>();
        for (IPortStorage storage : storages) {
            var handler = storage.getCapability(MekCapabilities.CHEMICAL);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    private List<IChemicalHandler> all() {
        var result = handlers(inputs.get());
        result.addAll(handlers(outputs.get()));
        return result;
    }

    @Override
    public int getChemicalTanks() {
        int tanks = 1;
        for (var handler : all()) {
            tanks += handler.getChemicalTanks();
        }
        return tanks;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        for (var handler : all()) {
            if (tank < handler.getChemicalTanks()) {
                return handler.getChemicalInTank(tank);
            }
            tank -= handler.getChemicalTanks();
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        for (var handler : all()) {
            if (tank < handler.getChemicalTanks()) {
                return handler.getChemicalTankCapacity(tank);
            }
            tank -= handler.getChemicalTanks();
        }
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return true;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        return tank == getChemicalTanks() - 1 ? insertChemical(stack, action) : stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        ChemicalStack remainder = stack;
        for (var handler : handlers(inputs.get())) {
            if (remainder.isEmpty()) {
                break;
            }
            remainder = handler.insertChemical(remainder, action);
        }
        return remainder;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        for (var handler : handlers(inputs.get())) {
            tank -= handler.getChemicalTanks();
        }
        if (tank < 0) {
            return ChemicalStack.EMPTY;
        }
        for (var handler : handlers(outputs.get())) {
            if (tank < handler.getChemicalTanks()) {
                return handler.extractChemical(tank, amount, action);
            }
            tank -= handler.getChemicalTanks();
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        for (var handler : handlers(outputs.get())) {
            var extracted = handler.extractChemical(amount, action);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        for (var handler : handlers(outputs.get())) {
            var extracted = handler.extractChemical(stack, action);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ChemicalStack.EMPTY;
    }
}
