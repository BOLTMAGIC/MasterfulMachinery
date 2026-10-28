package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.core.LevelKJS;
import dev.latvian.mods.kubejs.level.KubeLevelEvent;
import dev.latvian.mods.kubejs.level.LevelBlock;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MachineRecipeEventJS implements KubeLevelEvent {
    private final MachineControllerBlockEntity controller;
    private final String recipeId;

    public MachineRecipeEventJS(MachineControllerBlockEntity controller, String recipeId) {
        this.controller = controller;
        this.recipeId = recipeId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public MachineControllerBlockEntity getController() {
        return controller;
    }

    public String getControllerId() {
        return Ref.id(controller.getModel().id()).toString();
    }

    @Nullable
    public String getStructureId() {
        return controller.getStructure() == null ? null : controller.getStructure().id().toString();
    }

    @Override
    public Level getLevel() {
        return controller.getLevel();
    }

    public BlockPos getPos() {
        return controller.getBlockPos();
    }

    public LevelBlock getBlock() {
        return ((LevelKJS) controller.getLevel()).kjs$getBlock(controller.getBlockPos());
    }
}
