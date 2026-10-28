package io.ticticboom.mods.mm.compat.interop;

import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import net.minecraft.resources.Identifier;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.structure.StructureModel;

import java.util.List;

public interface IKubeJSInterop {
    List<StructureModel> postCreateStructures();
    List<RecipeModel> postCreateRecipes();
    List<ControllerModel> postRegisterControllers();
    List<PortModel> postRegisterPorts();
    List<ExtraBlockModel> postRegisterExtraBlocks();

    List<BuildableStructure> postBuilderStructures(List<BuildableStructure> loaded, Function<Identifier, CompoundTag> reader);

    boolean postRecipeStarted(MachineControllerBlockEntity controller, Identifier recipeId);

    void postRecipeFinished(MachineControllerBlockEntity controller, Identifier recipeId);

    /**
     * MMEvents.flexOutput: lets scripts pick what a flex output makes from an input item.
     *
     * @return null to keep the template result, an empty stack to not accept the input, else the output item
     */
    @org.jetbrains.annotations.Nullable
    default net.minecraft.world.item.ItemStack resolveFlexOutput(Identifier recipeId, String from,
                                                                 net.minecraft.world.item.ItemStack input,
                                                                 net.minecraft.world.item.ItemStack templateResult) {
        return null;
    }
}
