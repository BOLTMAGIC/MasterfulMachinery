package io.ticticboom.mods.mm.port.mekanism.gas;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.NotifyChangeContentsListener;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Predicate;

public class MekanismGasPortStorage extends MekanismChemicalPortStorage<Gas, GasStack> {

    protected MekanismGasPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        super(model, changed);
    }

    @Override
    protected IChemicalTank<Gas, GasStack> createTank(long capacity, Predicate<Gas> validator, INotifyChangeFunction changed) {
        return ChemicalTankBuilder.GAS.create(capacity, validator, new NotifyChangeContentsListener(changed));
    }

    @Override
    public Capability<IGasHandler> getChemicalCapability() {
        return MekCapabilities.GAS;
    }

    @Override
    protected IForgeRegistry<Gas> getChemicalRegistry() {
        return MekanismAPI.gasRegistry();
    }

    @Override
    protected JsonObject debugStack(GasStack stack) {
        var json = new JsonObject();
        json.addProperty("gas", stack.getType().getRegistryName().toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    @Override
    public <T> boolean hasCapability(Capability<T> capability) {
        return MekCapabilities.GAS == capability;
    }
}
