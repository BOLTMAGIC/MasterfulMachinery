package io.ticticboom.mods.mm.compat.interop;

import java.util.function.Function;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import io.ticticboom.mods.mm.compat.kjs.event.BuilderStructureEventJS;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureParser;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.resources.Identifier;
import io.ticticboom.mods.mm.compat.kjs.event.MachineRecipeEventJS;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.compat.kjs.MMKubeEvents;
import io.ticticboom.mods.mm.compat.kjs.builder.ControllerBuilderJS;
import io.ticticboom.mods.mm.compat.kjs.builder.ExtraBlockBuilderJS;
import io.ticticboom.mods.mm.compat.kjs.builder.RecipeBuilderJS;
import io.ticticboom.mods.mm.compat.kjs.builder.StructureBuilderJS;
import io.ticticboom.mods.mm.compat.kjs.event.*;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.structure.StructureModel;

import java.util.List;

public class MMKubeJSInterop implements IKubeJSInterop {
    @Override
    public List<StructureModel> postCreateStructures() {
        var event = new StructureEventJS();
        MMKubeEvents.STRUCTURES.post(event);
        return event.getBuilders().stream().map(StructureBuilderJS::build).toList();
    }

    @Override
    public List<RecipeModel> postCreateRecipes() {
        var event = new RecipeEventJS();
        MMKubeEvents.RECIPES.post(event);
        return event.getBuilders().stream().map(RecipeBuilderJS::build).toList();
    }

    @Override
    public List<ControllerModel> postRegisterControllers() {
        ControllerEventJS event = new ControllerEventJS();
        MMKubeEvents.CONTROLLERS.post(event);
        return event.getControllers().stream().filter(c -> c.getType() != null).map(ControllerBuilderJS::build).toList();
    }

    @Override
    public List<PortModel> postRegisterPorts() {
        var event = new PortEventJS();
        MMKubeEvents.PORTS.post(event);
        return event.getPorts().stream().filter(p -> p.getType() != null).flatMap(a -> a.build().stream()).toList();
    }

    @Override
    public List<ExtraBlockModel> postRegisterExtraBlocks() {
        var event = new ExtraBlockEventJS();
        MMKubeEvents.EXTRA.post(event);
        return event.getBuilder().stream().filter(b -> b.getType() != null).map(ExtraBlockBuilderJS::build).toList();
    }

    @Override
    public boolean postRecipeStarted(MachineControllerBlockEntity controller, Identifier recipeId) {
        if (!MMKubeEvents.RECIPE_STARTED.hasListeners()) {
            return true;
        }
        return !MMKubeEvents.RECIPE_STARTED.post(new MachineRecipeEventJS(controller, recipeId.toString()), recipeId).interruptFalse();
    }

    @Override
    public net.minecraft.world.item.ItemStack resolveFlexOutput(Identifier recipeId, String from,
                                                                net.minecraft.world.item.ItemStack input,
                                                                net.minecraft.world.item.ItemStack templateResult) {
        if (recipeId == null || !MMKubeEvents.FLEX_OUTPUT.hasListeners()) {
            return null;
        }
        var event = new io.ticticboom.mods.mm.compat.kjs.event.FlexOutputEventJS(recipeId.toString(), from, input, templateResult);
        MMKubeEvents.FLEX_OUTPUT.post(event, recipeId);
        return event.isChanged() ? event.getResult() : null;
    }

    @Override
    public void postRecipeFinished(MachineControllerBlockEntity controller, Identifier recipeId) {
        if (MMKubeEvents.RECIPE_FINISHED.hasListeners()) {
            MMKubeEvents.RECIPE_FINISHED.post(new MachineRecipeEventJS(controller, recipeId.toString()), recipeId);
        }
    }

    @Override
    public List<BuildableStructure> postBuilderStructures(List<BuildableStructure> loaded, Function<Identifier, CompoundTag> reader) {
        if (!MMKubeEvents.BUILDER_STRUCTURES.hasListeners()) {
            return loaded;
        }
        var event = new BuilderStructureEventJS();
        MMKubeEvents.BUILDER_STRUCTURES.post(event);
        Set<String> removed = new HashSet<>(event.getRemoved());
        Set<String> removedNamespaces = new HashSet<>(event.getRemovedNamespaces());
        Map<Identifier, BuildableStructure> result = new LinkedHashMap<>();
        for (BuildableStructure structure : loaded) {
            if (!removed.contains(structure.id().toString()) && !removedNamespaces.contains(structure.id().getNamespace())) {
                result.put(structure.id(), structure);
            }
        }
        event.getAdded().forEach((idText, fileText) -> {
            Identifier id = Identifier.tryParse(idText);
            Identifier file = Identifier.tryParse(fileText);
            CompoundTag nbt = id == null || file == null ? null : reader.apply(file);
            if (nbt == null) {
                Ref.LOG.warn("MMEvents.builderStructures: can't read {} for {}", fileText, idText);
                return;
            }
            var parsed = BuildableStructureParser.parse(id, nbt);
            if (parsed.structure() == null) {
                Ref.LOG.warn("MMEvents.builderStructures: {} skipped: {}", idText, parsed.problem());
                return;
            }
            result.put(id, parsed.structure());
        });
        return new ArrayList<>(result.values());
    }
}
