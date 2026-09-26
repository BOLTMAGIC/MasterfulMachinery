package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Mekanism chemical side of the input gateway, like {@link GatewayFluidHandler}: shows the tanks of the machine's
 * input ports of one chemical type (read only), followed by one always-empty tank to fill; inserted chemicals go on
 * to the input ports, filling them in order. Nothing can be extracted.
 * Only created when Mekanism is loaded.
 */
public abstract class GatewayChemicalHandler<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>>
        implements IChemicalHandler<CHEMICAL, STACK> {
    private final Supplier<List<IPortStorage>> inputs;
    private final Capability<? extends IChemicalHandler<CHEMICAL, STACK>> capability;

    protected GatewayChemicalHandler(Supplier<List<IPortStorage>> inputs, Capability<? extends IChemicalHandler<CHEMICAL, STACK>> capability) {
        this.inputs = inputs;
        this.capability = capability;
    }

    private List<IChemicalHandler<CHEMICAL, STACK>> handlers() {
        var result = new ArrayList<IChemicalHandler<CHEMICAL, STACK>>();
        for (IPortStorage storage : inputs.get()) {
            storage.getCapability(capability).resolve().ifPresent(result::add);
        }
        return result;
    }

    @Override
    public int getTanks() {
        int tanks = 1;
        for (var handler : handlers()) {
            tanks += handler.getTanks();
        }
        return tanks;
    }

    @Override
    public STACK getChemicalInTank(int tank) {
        for (var handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getChemicalInTank(tank);
            }
            tank -= handler.getTanks();
        }
        // the trailing entry tank
        return getEmptyStack();
    }

    @Override
    public void setChemicalInTank(int tank, STACK stack) {
    }

    @Override
    public long getTankCapacity(int tank) {
        for (var handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getTankCapacity(tank);
            }
            tank -= handler.getTanks();
        }
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, STACK stack) {
        return true;
    }

    @Override
    public STACK insertChemical(int tank, STACK stack, Action action) {
        // the port tanks are only shown; everything goes in through the entry tank
        return tank == getTanks() - 1 ? insertChemical(stack, action) : stack;
    }

    @Override
    public STACK insertChemical(STACK stack, Action action) {
        STACK remainder = stack;
        for (var handler : handlers()) {
            if (remainder.isEmpty()) {
                break;
            }
            remainder = handler.insertChemical(remainder, action);
        }
        return remainder;
    }

    @Override
    public STACK extractChemical(int tank, long amount, Action action) {
        return getEmptyStack();
    }

    /**
     * @return the gateway's handler for this capability, null when it isn't a Mekanism chemical one
     */
    @Nullable
    public static LazyOptional<?> create(Capability<?> cap, Supplier<List<IPortStorage>> inputs) {
        if (cap == MekCapabilities.GAS) return LazyOptional.of(() -> new Gases(inputs));
        if (cap == MekCapabilities.INFUSE) return LazyOptional.of(() -> new Infusions(inputs));
        if (cap == MekCapabilities.PIGMENT) return LazyOptional.of(() -> new Pigments(inputs));
        if (cap == MekCapabilities.SLURRY) return LazyOptional.of(() -> new Slurries(inputs));
        return null;
    }

    private static class Gases extends GatewayChemicalHandler<Gas, GasStack> implements IGasHandler {
        Gases(Supplier<List<IPortStorage>> inputs) {
            super(inputs, MekCapabilities.GAS);
        }
    }

    private static class Infusions extends GatewayChemicalHandler<InfuseType, InfusionStack> implements IInfusionHandler {
        Infusions(Supplier<List<IPortStorage>> inputs) {
            super(inputs, MekCapabilities.INFUSE);
        }
    }

    private static class Pigments extends GatewayChemicalHandler<Pigment, PigmentStack> implements IPigmentHandler {
        Pigments(Supplier<List<IPortStorage>> inputs) {
            super(inputs, MekCapabilities.PIGMENT);
        }
    }

    private static class Slurries extends GatewayChemicalHandler<Slurry, SlurryStack> implements ISlurryHandler {
        Slurries(Supplier<List<IPortStorage>> inputs) {
            super(inputs, MekCapabilities.SLURRY);
        }
    }
}
