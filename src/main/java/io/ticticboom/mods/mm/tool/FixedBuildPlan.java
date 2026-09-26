package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * What the multiblock tool builds for another mod's structure: its fixed blocks, rotated like an MM build (player
 * facing plus the tool's extra turns) and shifted so the block in front of the clicked face is the bottom-middle-front.
 * There is no controller and no tier choice. Common code: the server build and the client hologram share it.
 *
 * @param center     middle of the structure, for the job's distance check
 * @param plan       one step per block
 * @param obstructed planned positions holding something that is neither replaceable nor the planned block
 */
public record FixedBuildPlan(BlockPos center, AssemblyPlanner.Plan plan, List<BlockPos> obstructed) {

    public static FixedBuildPlan create(Level level, BuildableStructure structure, BlockPos clickedPos, Direction clickedFace,
                                        Direction playerFacing, int extraTurns) {
        Rotation rotation = ToolBuildPlan.rotation(playerFacing, extraTurns);
        BlockPos anchor = clickedPos.relative(clickedFace);

        List<BlockPos> rotated = new ArrayList<>(structure.blockCount());
        AABB bounds = null;
        for (BuildableStructure.Placement placement : structure.blocks()) {
            BlockPos pos = anchor.offset(placement.pos().rotate(rotation));
            rotated.add(pos);
            bounds = bounds == null ? new AABB(pos) : bounds.minmax(new AABB(pos));
        }
        BlockPos offset = bounds == null ? BlockPos.ZERO : StructurePasteUtil.offsetBottomMiddleFrontToAnchor(anchor, playerFacing, bounds);

        var steps = new ArrayList<AssemblyPlanner.Planned>(rotated.size());
        var obstructed = new ArrayList<BlockPos>();
        for (int i = 0; i < rotated.size(); i++) {
            BlockPos pos = rotated.get(i).offset(offset);
            BlockState state = structure.blocks().get(i).state().rotate(rotation);
            steps.add(new AssemblyPlanner.Planned(pos, state, List.of(state.getBlock())));
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && !existing.canBeReplaced() && !existing.is(state.getBlock())) {
                obstructed.add(pos);
            }
        }
        BlockPos center = bounds == null ? anchor : BlockPos.containing(bounds.move(offset.getX(), offset.getY(), offset.getZ()).getCenter());
        return new FixedBuildPlan(center, new AssemblyPlanner.Plan(List.copyOf(steps), 0), List.copyOf(obstructed));
    }
}
