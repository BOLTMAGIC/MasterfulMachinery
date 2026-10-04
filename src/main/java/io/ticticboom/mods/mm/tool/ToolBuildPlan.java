package io.ticticboom.mods.mm.tool;

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

/**
 * What the multiblock tool builds when used on a block face: the controller first, then every piece. Common code
 * (server build and client hologram share it), so both plan the same for the same inputs; the inputs differ, though,
 * where the ME network comes in: the client knows nothing of its stock ({@link #availableSnapshot}), so the hologram
 * may show a tier or block the server then takes from the network differently.
 *
 * @param controllerPos where the controller goes
 * @param rotation      the structure's rotation; the controller faces the way {@link AssemblyPlanner#rotationFor} maps
 *                      back to it
 * @param plan          controller step followed by the pieces
 * @param obstructed    planned positions holding something that is neither replaceable nor an accepted block
 */
public record ToolBuildPlan(BlockPos controllerPos, Rotation rotation, AssemblyPlanner.Plan plan, List<BlockPos> obstructed) {

    /** Player facing (same mapping as the blueprint) turned clockwise by the tool's extra quarter turns. */
    public static Rotation rotation(Direction playerFacing, int extraTurns) {
        return AssemblyPlanner.rotationFor(playerFacing).getRotated(Rotation.values()[Math.floorMod(extraTurns, 4)]);
    }

    /**
     * @param clickedPos   the block the tool was used on; the anchor is the block in front of {@code clickedFace}, and
     *                     it becomes the bottom-middle-front of the structure (like a blueprint paste)
     * @param playerFacing the player's horizontal facing, which also defines "front" for the anchor
     * @param available    blocks the player can supply, to pick among a position's candidates (see
     *                     {@link #availableSnapshot})
     * @return null when the structure has no registered controller block
     */
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
            // the inverse of rotationFor, so the controller's own Assemble later picks the same rotation
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

    /** The plan for this player using this tool: its extra turns and tier preferences, and what it and the player carry. */
    public static @Nullable ToolBuildPlan create(Level level, Player player, ItemStack tool, StructureModel model, BlockPos clickedPos, Direction clickedFace) {
        return create(level, player, tool, model, clickedPos, clickedFace, availableSnapshot(player, tool));
    }

    /** As above, with what is available already read (e.g. {@link ChainedMaterialSource#snapshot} of the build's source). */
    public static @Nullable ToolBuildPlan create(Level level, Player player, ItemStack tool, StructureModel model, BlockPos clickedPos, Direction clickedFace,
                                                 Predicate<Block> available) {
        return create(level, model, clickedPos, clickedFace, ToolData.placementFacing(tool, player), ToolData.extraTurns(tool), ToolData.tiers(tool), available);
    }

    /**
     * Blocks the tool's store, the player's inventory or (server side) the tool's ME network can supply (everything in
     * creative), read once: the store is deserialized a single time however many blocks are asked about.
     */
    public static Predicate<Block> availableSnapshot(Player player, ItemStack tool) {
        return source(player, tool).snapshot();
    }

    /**
     * Where a build with this tool draws blocks and energy from: store, inventory, then the bound ME network when it can
     * be reached (server only; the client knows nothing about the network's contents).
     */
    public static ChainedMaterialSource source(Player player, ItemStack tool) {
        MeAccess me = player instanceof ServerPlayer serverPlayer ? MeAccessFactory.forTool(serverPlayer, tool) : null;
        return source(player, tool, me);
    }

    /** As above, with the network given (or none). */
    public static ChainedMaterialSource source(Player player, ItemStack tool, @Nullable MeAccess me) {
        return new ChainedMaterialSource(new ToolStore(tool), player, energy(tool), me);
    }

    /** Where dismantled blocks go: store, then inventory (never the ME network). */
    public static ChainedMaterialSource sink(Player player, ItemStack tool) {
        return new ChainedMaterialSource(new ToolStore(tool), player, energy(tool));
    }

    private static ToolEnergy energy(ItemStack tool) {
        return new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get());
    }
}
