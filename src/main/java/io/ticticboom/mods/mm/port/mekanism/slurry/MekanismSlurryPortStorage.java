package io.ticticboom.mods.mm.port.mekanism.slurry;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.NotifyChangeContentsListener;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Predicate;

public class MekanismSlurryPortStorage extends MekanismChemicalPortStorage<Slurry, SlurryStack> {

    public MekanismSlurryPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        super(model, changed);
    }

    @Override
    protected IChemicalTank<Slurry, SlurryStack> createTank(long capacity, Predicate<Slurry> validator, INotifyChangeFunction changed) {
        return ChemicalTankBuilder.SLURRY.create(capacity, validator, new NotifyChangeContentsListener(changed));
    }

    @Override
    public Capability<ISlurryHandler> getChemicalCapability() {
        return MekCapabilities.SLURRY;
    }

    @Override
    protected IForgeRegistry<Slurry> getChemicalRegistry() {
        return MekanismAPI.slurryRegistry();
    }

    @Override
    protected JsonObject debugStack(SlurryStack stack) {
        var json = new JsonObject();
        json.addProperty("slurry", stack.getType().getRegistryName().toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }


    @Override
    public <T> boolean hasCapability(Capability<T> capability) {
        return MekCapabilities.SLURRY == capability;
    }
}
