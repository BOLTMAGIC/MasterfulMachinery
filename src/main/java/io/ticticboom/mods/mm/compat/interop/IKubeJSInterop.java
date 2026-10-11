package io.ticticboom.mods.mm.compat.interop;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.structure.StructureModel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface IKubeJSInterop {
    List<StructureModel> postCreateStructures();
    List<RecipeModel> postCreateRecipes();
    List<ControllerModel> postRegisterControllers();
    List<PortModel> postRegisterPorts();
    List<ExtraBlockModel> postRegisterExtraBlocks();

    /**
     * MMEvents.recipeStarted, just before a machine starts a recipe.
     * @return false if a script cancelled the start
     */
    default boolean onRecipeStart(MachineControllerBlockEntity controller, ResourceLocation recipeId) {
        return true;
    }

    /**
     * MMEvents.flexOutput: lets scripts pick what a flex output makes from an input item.
     *
     * @return null to keep the template result, an empty stack to not accept the input, else the output item
     */
    @Nullable
    default ItemStack resolveFlexOutput(ResourceLocation recipeId, String from, ItemStack input, ItemStack templateResult) {
        return null;
    }

    /** MMEvents.recipeFinished, after a machine has produced a recipe's outputs. */
    default void onRecipeFinish(MachineControllerBlockEntity controller, ResourceLocation recipeId) {
    }

    /**
     * MMEvents.builderStructures, after the multiblock tool's non-MM structures were loaded: scripts may remove some
     * and add more from other {@code .nbt} files.
     *
     * @param reader reads a structure {@code .nbt} by its resource location (e.g. {@code pack:structures/foo.nbt}),
     *               null when it can't
     * @return the structures to keep
     */
    default List<BuildableStructure> postBuilderStructures(List<BuildableStructure> loaded, Function<ResourceLocation, CompoundTag> reader) {
        return loaded;
    }
}
