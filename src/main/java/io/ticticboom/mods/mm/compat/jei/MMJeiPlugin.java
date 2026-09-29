package io.ticticboom.mods.mm.compat.jei;

import com.google.common.collect.ImmutableList;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.tool.MultiblockToolScreen;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerScreen;
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
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@JeiPlugin
public class MMJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = Ref.id("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    public static final List<MMRecipeCategory> recipeCategories = new ArrayList<>();

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        JeiRecipeLookup.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeLookup.setRuntime(null);
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
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
            recipes = MachineRecipeManager.RECIPES.values().stream()
                    .filter(x -> x.allStructureIds().contains(parent.id())).collect(Collectors.toList());
        } else {
            recipes = new ArrayList<>(MachineRecipeManager.RECIPES.values());
        }
        int maxInputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(r.inputs().inputs().size())).max().orElse(1);
        int maxOutputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(
                r.outputs().outputs().stream().mapToInt(o -> o.displayedOutputs().size()).sum())).max().orElse(1);
        int maxRows = Math.max(maxInputRows, maxOutputRows);
        int height = Math.max(40, Math.min(maxRows, MMRecipeCategory.VISIBLE_ROWS) * 20);
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
                        .filter(x -> x.allStructureIds().contains(entry.getStructureModel().id()))
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

    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        // the multiblock tool screen's tabs hang outside its window; keep JEI's item list off them
        registration.addGuiContainerHandler(MultiblockToolScreen.class, new IGuiContainerHandler<>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull MultiblockToolScreen screen) {
                return screen.getTabAreas();
            }

            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, MultiblockToolScreen screen, double mouseX, double mouseY) {
                ItemStack stack = screen.getJeiMaterialAt(mouseX, mouseY);
                Rect2i area = screen.getJeiMaterialAreaAt(mouseX, mouseY);
                if (stack == null || stack.isEmpty() || area == null) return Optional.empty();
                return factory.createBuilder(stack).buildWithArea(area);
            }
        });
        registration.addGuiContainerHandler(MachineControllerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, MachineControllerScreen screen, double mouseX, double mouseY) {
                var hovered = screen.getJeiIngredientAt(mouseX, mouseY);
                if (hovered == null) return Optional.empty();
                var content = hovered.content();
                return switch (content.kind()) {
                    case ITEM -> factory.createBuilder(content.item().copyWithCount(1)).buildWithArea(hovered.area());
                    case FLUID -> factory.createBuilder(ForgeTypes.FLUID_STACK, content.fluid().copy()).buildWithArea(hovered.area());
                    default -> Optional.empty();
                };
            }
        });
    }
}
