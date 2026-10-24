package io.ticticboom.mods.mm.port.mekanism.pigment;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

public class MekanismPigmentPortIngredient extends MekanismChemicalPortIngredient<Pigment, PigmentStack> {

    public MekanismPigmentPortIngredient(ResourceLocation chemical, long amount) {
        super(chemical, amount);
    }

    @Override
    public io.ticticboom.mods.mm.port.IPortIngredient scaled(double factor) {
        return new MekanismPigmentPortIngredient(id, io.ticticboom.mods.mm.port.IPortIngredient.scaleAmount(amount, factor));
    }

    @Override
    public PigmentStack createStack(Pigment id, long amount) {
        return new PigmentStack(id, amount);
    }

    @Override
    public Pigment findChemical(ResourceLocation id) {
        return MekanismAPI.pigmentRegistry().getValue(id);
    }

    @Override
    public Class<? extends MekanismChemicalPortStorage<Pigment, PigmentStack>> getStorageClass() {
        return MekanismPigmentPortStorage.class;
    }

    @Override
    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_PIGMENT;
    }

    @Override
    protected IIngredientType<PigmentStack> jeiType() {
        return MekanismJEI.TYPE_PIGMENT;
    }
}
