package io.ticticboom.mods.mm.port.mekanism.slurry;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

public class MekanismSlurryPortIngredient extends MekanismChemicalPortIngredient<Slurry, SlurryStack> {

    public MekanismSlurryPortIngredient(ResourceLocation chemical, long amount) {
        super(chemical, amount);
    }

    @Override
    public io.ticticboom.mods.mm.port.IPortIngredient scaled(double factor) {
        return new MekanismSlurryPortIngredient(id, io.ticticboom.mods.mm.port.IPortIngredient.scaleAmount(amount, factor));
    }

    @Override
    public SlurryStack createStack(Slurry id, long amount) {
        return new SlurryStack(id, amount);
    }

    @Override
    public Slurry findChemical(ResourceLocation id) {
        return MekanismAPI.slurryRegistry().getValue(id);
    }

    @Override
    public Class<? extends MekanismChemicalPortStorage<Slurry, SlurryStack>> getStorageClass() {
        return MekanismSlurryPortStorage.class;
    }

    @Override
    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_SLURRY;
    }

    @Override
    protected IIngredientType<SlurryStack> jeiType() {
        return MekanismJEI.TYPE_SLURRY;
    }
}
