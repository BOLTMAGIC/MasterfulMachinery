package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum ToolBuildMode {
    SEQUENTIAL, LAYER_BY_LAYER, INSTANT;

    /** Keep the existing per-tool instant option in sequential mode; explicit server modes take precedence. */
    public ToolBuildMode forTool(boolean instantPreference) {
        return this == SEQUENTIAL && instantPreference ? INSTANT : this;
    }

    public AssemblyPlanner.Plan order(AssemblyPlanner.Plan plan, BlockPos controllerPos, boolean requiresController) {
        if (this != LAYER_BY_LAYER || plan.steps().isEmpty()) return plan;
        var steps = new ArrayList<>(plan.steps());
        AssemblyPlanner.Planned controller = requiresController && steps.get(0).pos().equals(controllerPos) ? steps.remove(0) : null;
        steps.sort(Comparator.comparingInt((AssemblyPlanner.Planned step) -> step.pos().getY())
                .thenComparingInt(step -> step.pos().getX()).thenComparingInt(step -> step.pos().getZ()));
        if (controller != null) steps.add(0, controller);
        return new AssemblyPlanner.Plan(List.copyOf(steps), plan.unavailable());
    }

    public int budget(int blocksPerTick) {
        return this == INSTANT ? Integer.MAX_VALUE : blocksPerTick;
    }
}
