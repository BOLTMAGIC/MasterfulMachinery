package io.ticticboom.mods.mm.port.mekanism.gas;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

public class MekanismGasPortIngredient extends MekanismChemicalPortIngredient<Gas, GasStack> {

    public MekanismGasPortIngredient(ResourceLocation chemical, long amount) {
        super(chemical, amount);
    }

    @Override
    public io.ticticboom.mods.mm.port.IPortIngredient scaled(double factor) {
        return new MekanismGasPortIngredient(id, io.ticticboom.mods.mm.port.IPortIngredient.scaleAmount(amount, factor));
    }

    @Override
    public GasStack createStack(Gas id, long amount) {
        return new GasStack(id, amount);
    }

    @Override
    public Gas findChemical(ResourceLocation id) {
        return MekanismAPI.gasRegistry().getValue(id);
    }

    @Override
    public Class<? extends MekanismChemicalPortStorage<Gas, GasStack>> getStorageClass() {
        return MekanismGasPortStorage.class;
    }

    @Override
    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_GAS;
    }

    @Override
    protected IIngredientType<GasStack> jeiType() {
        return MekanismJEI.TYPE_GAS;
    }
}
