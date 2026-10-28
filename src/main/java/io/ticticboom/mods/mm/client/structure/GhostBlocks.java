package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.client.render.BlockModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class GhostBlocks {
    private static final int GHOST_COLOR = 0x66FFFFFF;

    private GhostBlocks() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Map<BlockPos, BlockState> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        var type = RenderTypes.translucentMovingBlock();
        var ghost = buffers.getBuffer(type);
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.scale(0.98F, 0.98F, 0.98F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            BlockModels.draw(poseStack.last(), ghost, entry.getValue(), GHOST_COLOR, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }
}
