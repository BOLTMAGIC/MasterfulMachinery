package io.ticticboom.mods.mm.compat.jei.ingredient;

import io.ticticboom.mods.mm.compat.jei.ingredient.create.CreateRotationIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.pncr.PneumaticAirIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredientType;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceIngredientType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class MMJeiIngredients {
    public static final EnergyIngredientType ENERGY = new EnergyIngredientType();
    public static final IIngredientType<FluidStack> FLUID = NeoForgeTypes.FLUID_STACK;
    public static final IIngredientType<ItemStack> ITEM = VanillaTypes.ITEM_STACK;
    public static final PneumaticAirIngredientType PNEUMATIC_AIR = new PneumaticAirIngredientType();
    public static final BotaniaManaIngredientType BOTANIA_MANA = new BotaniaManaIngredientType();
    public static final CreateRotationIngredientType CREATE_ROTATION = new CreateRotationIngredientType();
    public static final HeatIngredientType MEKANISM_HEAT = new HeatIngredientType();
    public static final MatterIngredientType REPLICATION_MATTER = new MatterIngredientType();
    public static final EntityIngredientType ENTITY = new EntityIngredientType();
    public static final RadiationIngredientType NUCLEAR_RADIATION = new RadiationIngredientType();
    public static final ArsSourceIngredientType ARS_SOURCE = new ArsSourceIngredientType();
    public static final EmcIngredientType PROJECTE_EMC = new EmcIngredientType();
}
