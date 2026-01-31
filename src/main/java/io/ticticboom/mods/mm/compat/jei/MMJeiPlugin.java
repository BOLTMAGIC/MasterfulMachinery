package io.ticticboom.mods.mm.compat.jei;

import com.google.common.collect.ImmutableList;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.category.MMRecipeCategory;
import io.ticticboom.mods.mm.compat.jei.category.MMStructureCategory;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.create.CreateRotationIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.create.CreateRotationIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.pncr.PneumaticAirIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.pncr.PneumaticAirIngredientRender;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JeiPlugin
public class MMJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = Ref.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    public static final List<MMRecipeCategory> recipeCategories = new ArrayList<>();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (MMConfig.JEI_RECIPE_SPLIT) {
            for (StructureModel parentStructure : StructureManager.STRUCTURES.values()) {
                registerProcessRecipe(registration, parentStructure);
            }
        } else {
            registerProcessRecipe(registration, null);
        }
        registration.addRecipeCategories(new MMStructureCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private void registerProcessRecipe(IRecipeCategoryRegistration registration, StructureModel parent) {
        List<RecipeModel> recipes;
        if (parent != null) {
            recipes = MachineRecipeManager.RECIPES.values().stream().filter(x -> x.structureId().equals(parent.id())).collect(Collectors.toList());
        } else {
            recipes = new ArrayList<>(MachineRecipeManager.RECIPES.values());
        }
        int maxInputRows = recipes.stream().mapToInt(r -> (int) Math.ceil(r.inputs().inputs().size() / 3.0)).max().orElse(1);
        int maxOutputRows = recipes.stream().mapToInt(r -> (int) Math.ceil(r.outputs().outputs().size() / 3.0)).max().orElse(1);
        int maxRows = Math.max(maxInputRows, maxOutputRows);
        int height = maxRows * 16 + 20; // Padding für Progressbar etc.
        MMRecipeCategory category = new MMRecipeCategory(registration.getJeiHelpers(), parent, height);
        registration.addRecipeCategories(category);
        recipeCategories.add(category);
    }

    // Java
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (MMConfig.JEI_RECIPE_SPLIT) {
            for (var entry : recipeCategories) {
                var recipes = MachineRecipeManager.RECIPES.values().stream()
                        .filter(x -> x.structureId().equals(entry.getStructureModel().id()))
                        .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                        .toList();
                registration.addRecipes(entry.getRecipeType(), recipes);
            }
        } else {
            var sorted = MachineRecipeManager.RECIPES.values().stream()
                    .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                    .collect(Collectors.toList());
            registration.addRecipes(MMRecipeCategory.RECIPE_TYPE, sorted);
        }

        var sortedStructures = StructureManager.STRUCTURES.values().stream()
                .sorted(java.util.Comparator.comparing(s -> s.id().toString()))
                .collect(Collectors.toList());
        registration.addRecipes(MMStructureCategory.RECIPE_TYPE, sortedStructures);
    }


    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(MMJeiIngredients.ENERGY, ImmutableList.of(), new EnergyIngredientHelper(), new EnergyIngredientRenderer());
        registration.register(MMJeiIngredients.PNEUMATIC_AIR, ImmutableList.of(), new PneumaticAirIngredientHelper(), new PneumaticAirIngredientRender());
        registration.register(MMJeiIngredients.BOTANIA_MANA, ImmutableList.of(), new BotaniaManaIngredientHelper(), new BotaniaManaIngredientRenderer());
        registration.register(MMJeiIngredients.CREATE_ROTATION, ImmutableList.of(), new CreateRotationIngredientHelper(), new CreateRotationIngredientRenderer());
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (var entry : recipeCategories) {
            ResourceLocation location = entry.getStructureModel().controllerIds().getIds().get(0);
            ItemStack stack = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(location)).getDefaultInstance();
            registration.addRecipeCatalyst(stack,entry.getRecipeType());
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(MMRegisters.BLUEPRINT.get());
    }
}