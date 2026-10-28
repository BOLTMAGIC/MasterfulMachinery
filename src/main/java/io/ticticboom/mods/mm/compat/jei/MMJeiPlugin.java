package io.ticticboom.mods.mm.compat.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientRenderer;
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
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.*;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcIngredientRenderer;
import io.ticticboom.mods.mm.util.ItemNbtUtil;

@SuppressWarnings("unused")
@JeiPlugin
public class MMJeiPlugin implements IModPlugin {

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRecipeLookup.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeLookup.setRuntime(null);
    }

    public static final Identifier UID = Ref.id("jei_plugin");

    @Override
    public @NotNull Identifier getPluginUid() {
        return UID;
    }

    public static final List<MMRecipeCategory> recipeCategories = new ArrayList<>();

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        recipeCategories.clear();
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
            recipes = MachineRecipeManager.RECIPES.values().stream().filter(x -> x.runsIn(parent.id())).collect(Collectors.toList());
        } else {
            recipes = new ArrayList<>(MachineRecipeManager.RECIPES.values());
        }
        int maxInputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(MMRecipeCategory.inputSlotCount(r))).max().orElse(1);
        int maxOutputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(MMRecipeCategory.outputSlotCount(r))).max().orElse(1);
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
                        .filter(x -> x.runsIn(entry.getStructureModel().id()))
                        .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                        .flatMap(r -> FlexJeiPages.expand(r).stream())
                        .toList();
                registration.addRecipes(entry.getRecipeType(), recipes);
            }
        } else {
            var sorted = MachineRecipeManager.RECIPES.values().stream()
                    .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                    .flatMap(r -> FlexJeiPages.expand(r).stream())
                    .collect(Collectors.toList());
            registration.addRecipes(MMRecipeCategory.RECIPE_TYPE, sorted);
        }

        var sortedStructures = StructureManager.buildableStructures().stream()
                .sorted(java.util.Comparator.comparing(s -> s.id().toString()))
                .collect(Collectors.toList());
        registration.addRecipes(MMStructureCategory.RECIPE_TYPE, sortedStructures);
    }


    /**
     * JEI 29 saves bookmarked ingredients through a codec. MM's own ingredient types (energy, mana...) are display
     * only, so they encode to their type name and are not restored.
     */
    private static <V> Codec<V> unsaved(String name) {
        return Codec.STRING.comapFlatMap(s -> DataResult.error(() -> "MM " + name + " ingredients are not saved"), v -> name);
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(MMJeiIngredients.ENERGY, ImmutableList.of(), new EnergyIngredientHelper(), new EnergyIngredientRenderer(), unsaved("energy"));
        registration.register(MMJeiIngredients.PNEUMATIC_AIR, ImmutableList.of(), new PneumaticAirIngredientHelper(), new PneumaticAirIngredientRender(), unsaved("pneumatic_air"));
        registration.register(MMJeiIngredients.BOTANIA_MANA, ImmutableList.of(), new BotaniaManaIngredientHelper(), new BotaniaManaIngredientRenderer(), unsaved("botania_mana"));
        registration.register(MMJeiIngredients.CREATE_ROTATION, ImmutableList.of(), new CreateRotationIngredientHelper(), new CreateRotationIngredientRenderer(), unsaved("create_rotation"));
        registration.register(MMJeiIngredients.MEKANISM_HEAT, ImmutableList.of(), new HeatIngredientHelper(), new HeatIngredientRenderer(), unsaved("mekanism_heat"));
        registration.register(MMJeiIngredients.REPLICATION_MATTER, ImmutableList.of(), new MatterIngredientHelper(), new MatterIngredientRenderer(), unsaved("replication_matter"));
        registration.register(MMJeiIngredients.NUCLEAR_RADIATION, ImmutableList.of(), new RadiationIngredientHelper(), new RadiationIngredientRenderer(), unsaved("nuclear_radiation"));
        registration.register(MMJeiIngredients.ENTITY, ImmutableList.of(), new EntityIngredientHelper(), new EntityIngredientRenderer(), unsaved("entity"));
        registration.register(MMJeiIngredients.ARS_SOURCE, ImmutableList.of(), new ArsSourceIngredientHelper(), new ArsSourceIngredientRenderer(), unsaved("ars_source"));
        registration.register(MMJeiIngredients.PROJECTE_EMC, ImmutableList.of(), new EmcIngredientHelper(), new EmcIngredientRenderer(), unsaved("projecte_emc"));
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (var entry : recipeCategories) {
            StructureModel structure = entry.getStructureModel();
            if (structure == null) {
                continue;
            }
            List<Identifier> controllerIds = structure.controllerIds().getIds();
            if (controllerIds.isEmpty()) {
                Ref.LOG.error("Skipping JEI catalyst for structure {}: it lists no controllerIds.", structure.id());
                continue;
            }
            Identifier location = controllerIds.get(0);
            ItemStack stack = BuiltInRegistries.ITEM.getValue(location).getDefaultInstance();
            if (stack.isEmpty()) {
                Ref.LOG.error("Skipping JEI catalyst for structure {}: no controller block '{}' exists. "
                        + "Either it is misspelled, or the controller it names was not loaded.", structure.id(), location);
                continue;
            }
            registration.addRecipeCatalyst(stack, entry.getRecipeType());
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(MMRegisters.BLUEPRINT.get(),
                (stack, ctx) -> String.valueOf(ItemNbtUtil.getTag(stack)));
    }

    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
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
                    case FLUID -> factory.createBuilder(NeoForgeTypes.FLUID_STACK, content.fluid().copy()).buildWithArea(hovered.area());
                    default -> Optional.empty();
                };
            }
        });
    }
}
