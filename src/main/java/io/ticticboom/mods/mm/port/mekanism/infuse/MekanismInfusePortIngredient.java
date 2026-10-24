package io.ticticboom.mods.mm.port.mekanism.infuse;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

public class MekanismInfusePortIngredient extends MekanismChemicalPortIngredient<InfuseType, InfusionStack> {

    public MekanismInfusePortIngredient(ResourceLocation chemical, long amount) {
        super(chemical, amount);
    }

    @Override
    public io.ticticboom.mods.mm.port.IPortIngredient scaled(double factor) {
        return new MekanismInfusePortIngredient(id, io.ticticboom.mods.mm.port.IPortIngredient.scaleAmount(amount, factor));
    }

    @Override
    public InfusionStack createStack(InfuseType id, long amount) {
        return new InfusionStack(id, amount);
    }

    @Override
    public InfuseType findChemical(ResourceLocation id) {
        return MekanismAPI.infuseTypeRegistry().getValue(id);
    }

    @Override
    public Class<? extends MekanismChemicalPortStorage<InfuseType, InfusionStack>> getStorageClass() {
        return MekanismInfusePortStorage.class;
    }

    @Override
    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_INFUSE;
    }

    @Override
    protected IIngredientType<InfusionStack> jeiType() {
        return MekanismJEI.TYPE_INFUSION;
    }
}
