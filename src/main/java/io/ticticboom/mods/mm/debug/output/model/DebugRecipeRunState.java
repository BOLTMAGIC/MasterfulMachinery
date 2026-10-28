package io.ticticboom.mods.mm.debug.output.model;

import net.minecraft.resources.Identifier;

import java.util.List;

public class DebugRecipeRunState {
    private final Identifier recipeId;
    private final List<DebugRecipeRunEntry> inputs;
    private final List<DebugRecipeRunEntry> outputs;

    public DebugRecipeRunState(Identifier recipeId, List<DebugRecipeRunEntry> inputs, List<DebugRecipeRunEntry> outputs) {
        this.recipeId = recipeId;
        this.inputs = inputs;
        this.outputs = outputs;
    }
}
