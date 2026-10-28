package io.ticticboom.mods.mm.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Shared target for the hologram, anchor and server placement; never extends vanilla reach for other items. */
public record ToolTarget(BlockPos pos, Direction face, Direction facing) {
    public ToolTarget {
        pos = pos.immutable();
    }

    public static boolean hasSelection(ItemStack tool) {
        return ToolData.structure(tool) != null || ToolData.builderStructure(tool) != null;
    }

    @Nullable
    public static ToolTarget resolve(Player player, ItemStack tool, int range) {
        ToolData.Anchor anchor = ToolData.anchor(tool);
        if (anchor != null) {
            return anchor.dimension().equals(player.level().dimension().location()) && withinRange(player, anchor.pos(), range)
                    && usablePosition(player.level(), anchor.pos())
                    ? new ToolTarget(anchor.pos(), anchor.face(), anchor.facing()) : null;
        }
        return trace(player, range);
    }

    @Nullable
    public static ToolTarget trace(Player player, int range) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye;
        // Stop before unloaded chunks: Level.clip must never generate or load distant terrain.
        for (int distance = 1; distance <= range; distance++) {
            Vec3 sample = eye.add(view.scale(distance));
            if (!usablePosition(level, BlockPos.containing(sample))) break;
            end = sample;
        }
        boolean loaded = BlockGetter.traverseBlocks(eye, end, level,
                (world, pos) -> usablePosition(world, pos) ? null : Boolean.FALSE, world -> Boolean.TRUE);
        if (!loaded) return null;
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && usablePosition(level, hit.getBlockPos())
                ? new ToolTarget(hit.getBlockPos(), hit.getDirection(), player.getDirection()) : null;
    }

    public static boolean withinRange(Player player, BlockPos pos, int range) {
        // A hit can be on the near face; allow the remaining distance to that block's center.
        return player.getEyePosition().distanceToSqr(pos.getCenter()) <= (range + 1.0D) * (range + 1.0D);
    }

    public static boolean usablePosition(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
}
