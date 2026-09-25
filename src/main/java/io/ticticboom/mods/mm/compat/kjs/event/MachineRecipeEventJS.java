package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * MMEvents.recipeStarted / MMEvents.recipeFinished: a machine starts or finishes a recipe.
 * Both can be filtered by recipe id, e.g. {@code MMEvents.recipeFinished('mm:my_recipe', event => ...)}.
 * recipeStarted can be cancelled with {@code event.cancel()} to keep the recipe from starting.
 */
public class MachineRecipeEventJS extends ServerEventJS {
    private final MachineControllerBlockEntity controller;
    private final String recipeId;

    public MachineRecipeEventJS(MachineControllerBlockEntity controller, String recipeId) {
        super(controller.getLevel().getServer());
        this.controller = controller;
        this.recipeId = recipeId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public MachineControllerBlockEntity getController() {
        return controller;
    }

    /** The controller's id, e.g. "mm:auto_sieve". */
    public String getControllerId() {
        return Ref.id(controller.getModel().id()).toString();
    }

    /** The formed structure's id, or null if the machine isn't formed. */
    @Nullable
    public String getStructureId() {
        return controller.getStructure() == null ? null : controller.getStructure().id().toString();
    }

    public Level getLevel() {
        return controller.getLevel();
    }

    public BlockPos getPos() {
        return controller.getBlockPos();
    }

    /** The controller block, with KubeJS helpers (getDimension(), getPlayersInRadius(...), ...). */
    public BlockContainerJS getBlock() {
        return new BlockContainerJS(controller.getLevel(), controller.getBlockPos());
    }
}
