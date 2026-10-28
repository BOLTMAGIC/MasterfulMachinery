package io.ticticboom.mods.mm.controller.machine.register;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

public enum ControllerState implements StringRepresentable {
    UNFORMED("unformed"),
    IDLE("idle"),
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
