package io.ticticboom.mods.mm.compat.kjs;

import io.ticticboom.mods.mm.compat.kjs.event.BuilderStructureEventJS;
import net.minecraft.resources.Identifier;
import io.ticticboom.mods.mm.compat.kjs.event.MachineRecipeEventJS;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import io.ticticboom.mods.mm.compat.kjs.event.*;

public interface MMKubeEvents {
    EventGroup GROUP = EventGroup.of("MMEvents");

    EventHandler CONTROLLERS = GROUP.startup("registerControllers", () -> ControllerEventJS.class);
    EventHandler PORTS = GROUP.startup("registerPorts", () -> PortEventJS.class);
    EventHandler EXTRA = GROUP.startup("registerExtraBlocks", () -> ExtraBlockEventJS.class);
    EventHandler STRUCTURES = GROUP.server("createStructures", () -> StructureEventJS.class);
    EventHandler RECIPES = GROUP.server("createProcesses", () -> RecipeEventJS.class);
    EventHandler BUILDER_STRUCTURES = GROUP.server("builderStructures", () -> BuilderStructureEventJS.class);
    TargetedEventHandler<Identifier> RECIPE_STARTED = GROUP.server("recipeStarted", () -> MachineRecipeEventJS.class).supportsTarget(EventTargetType.ID).hasResult();
    TargetedEventHandler<Identifier> RECIPE_FINISHED = GROUP.server("recipeFinished", () -> MachineRecipeEventJS.class).supportsTarget(EventTargetType.ID);
    TargetedEventHandler<Identifier> FLEX_OUTPUT = GROUP.server("flexOutput", () -> FlexOutputEventJS.class).supportsTarget(EventTargetType.ID);

    // KubeJS 2101 registers groups through a registry handed to the plugin rather than
    // letting the group register itself.
    static void register(EventGroupRegistry registry) {
        registry.register(GROUP);
    }
}
