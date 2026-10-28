package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.ChainedMaterialSource;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record ToolBuildPlan(BlockPos controllerPos, Rotation rotation, AssemblyPlanner.Plan plan, List<BlockPos> obstructed) {

    public static Rotation rotation(Direction playerFacing, int extraTurns) {
        return AssemblyPlanner.rotationFor(playerFacing).getRotated(Rotation.values()[Math.floorMod(extraTurns, 4)]);
    }

    public static @Nullable ToolBuildPlan create(Level level, StructureModel model, BlockPos clickedPos, Direction clickedFace,
                                                 Direction playerFacing, int extraTurns, TierPrefs prefs, Predicate<Block> available) {
        Block controllerBlock = StructurePasteUtil.findControllerBlock(model);
        if (controllerBlock == null) {
            return null;
        }
        Rotation rotation = rotation(playerFacing, extraTurns);
        BlockPos controllerPos = StructurePasteUtil.createPlanForPlacementAnchor(model, clickedPos.relative(clickedFace), rotation, playerFacing).controllerPos();

        BlockState controllerState = controllerBlock.defaultBlockState();
        if (controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            controllerState = controllerState.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(Direction.NORTH));
        }
        AssemblyPlanner.Plan pieces = AssemblyPlanner.plan(model, controllerPos, rotation, prefs, available);
        var steps = new ArrayList<AssemblyPlanner.Planned>(pieces.steps().size() + 1);
        steps.add(new AssemblyPlanner.Planned(controllerPos, controllerState, List.of(controllerBlock)));
        steps.addAll(pieces.steps());

        var obstructed = new ArrayList<BlockPos>();
        for (AssemblyPlanner.Planned step : steps) {
            if (!ToolTarget.usablePosition(level, step.pos())) {
                obstructed.add(step.pos());
                continue;
            }
            BlockState existing = level.getBlockState(step.pos());
            boolean free = existing.isAir() || existing.canBeReplaced()
                    || existing.is(step.state().getBlock()) || step.accepted().contains(existing.getBlock());
            if (!free) {
                obstructed.add(step.pos());
            }
        }
        return new ToolBuildPlan(controllerPos, rotation, new AssemblyPlanner.Plan(List.copyOf(steps), pieces.unavailable()), List.copyOf(obstructed));
    }

    public static @Nullable ToolBuildPlan create(Level level, Player player, ItemStack tool, StructureModel model, BlockPos clickedPos, Direction clickedFace) {
        return create(level, player, tool, model, clickedPos, clickedFace, availableSnapshot(player, tool));
    }

    public static @Nullable ToolBuildPlan create(Level level, Player player, ItemStack tool, StructureModel model, BlockPos clickedPos, Direction clickedFace,
                                                 Predicate<Block> available) {
        return create(level, model, clickedPos, clickedFace, ToolData.placementFacing(tool, player), ToolData.extraTurns(tool), ToolData.tiers(tool), available);
    }

    public static Predicate<Block> availableSnapshot(Player player, ItemStack tool) {
        return source(player, tool).snapshot();
    }

    public static ChainedMaterialSource source(Player player, ItemStack tool) {
        MeAccess me = player instanceof ServerPlayer serverPlayer ? MeAccessFactory.forTool(serverPlayer, tool) : null;
        return source(player, tool, me);
    }

    public static ChainedMaterialSource source(Player player, ItemStack tool, @Nullable MeAccess me) {
        return new ChainedMaterialSource(new ToolStore(tool, player.level().registryAccess()), player, energy(tool), me);
    }

    public static ChainedMaterialSource sink(Player player, ItemStack tool) {
        return new ChainedMaterialSource(new ToolStore(tool, player.level().registryAccess()), player, energy(tool));
    }

    private static ToolEnergy energy(ItemStack tool) {
        return ToolEnergy.of(tool);
    }
}
