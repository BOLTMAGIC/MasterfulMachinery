package io.ticticboom.mods.mm.port.mekanism.infuse;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.NotifyChangeContentsListener;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Predicate;

public class MekanismInfusePortStorage extends MekanismChemicalPortStorage<InfuseType, InfusionStack> {

    protected MekanismInfusePortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        super(model, changed);
    }

    @Override
    protected IChemicalTank<InfuseType, InfusionStack> createTank(long capacity, Predicate<InfuseType> validator, INotifyChangeFunction changed) {
        return ChemicalTankBuilder.INFUSION.create(capacity, validator, new NotifyChangeContentsListener(changed));
    }

    @Override
    public Capability<IInfusionHandler> getChemicalCapability() {
        return MekCapabilities.INFUSE;
    }

    @Override
    protected IForgeRegistry<InfuseType> getChemicalRegistry() {
        return MekanismAPI.infuseTypeRegistry();
    }

    @Override
    protected JsonObject debugStack(InfusionStack stack) {
        var json = new JsonObject();
        json.addProperty("infuse", stack.getType().getRegistryName().toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    @Override
    public <T> boolean hasCapability(Capability<T> capability) {
        return MekCapabilities.INFUSE == capability;
    }
}
