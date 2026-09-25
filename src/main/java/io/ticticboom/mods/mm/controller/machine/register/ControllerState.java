package io.ticticboom.mods.mm.controller.machine.register;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

/**
 * What a machine controller is doing, kept in its block state so the model (and its screen color) follow it.
 */
public enum ControllerState implements StringRepresentable {
    /** the multiblock around the controller is not built */
    UNFORMED("unformed"),
    /** built, but no recipe is running (waiting for inputs, output full or stopped by redstone) */
    IDLE("idle"),
    /** built and processing a recipe */
    WORKING("working");

    public static final EnumProperty<ControllerState> PROPERTY = EnumProperty.create("state", ControllerState.class);

    private final String name;

    ControllerState(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
