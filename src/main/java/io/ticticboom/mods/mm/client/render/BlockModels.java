package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a block state's model quads into a vertex consumer, the way BlockRenderDispatcher.renderSingleBlock did before
 * 26.1 replaced baked models with BlockStateModel parts.
 */
public final class BlockModels {
    private static final Direction[] SIDES = Direction.values();

    private BlockModels() {
    }

    /** @param color ARGB multiplied into the model's colours (alpha makes ghosts) */
    public static void draw(PoseStack.Pose pose, VertexConsumer consumer, BlockState state, int color, int light) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        emit(pose, consumer, parts, color, light);
    }

    /** As draw, with the model's parts chosen for a block in a (preview) level, e.g. connected textures. */
    public static void draw(PoseStack.Pose pose, VertexConsumer consumer, BlockAndTintGetter level, BlockPos pos, BlockState state, int color, int light) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, pos, state, RandomSource.create(42L), parts);
        emit(pose, consumer, parts, color, light);
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer consumer, List<BlockStateModelPart> parts, int color, int light) {
        var instance = new QuadInstance();
        instance.setColor(color);
        instance.setLightCoords(light);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        for (BlockStateModelPart part : parts) {
            for (Direction side : SIDES) {
                for (var quad : part.getQuads(side)) {
                    consumer.putBakedQuad(pose, quad, instance);
                }
            }
            for (var quad : part.getQuads(null)) {
                consumer.putBakedQuad(pose, quad, instance);
            }
        }
    }

    /** LevelRenderer.renderLineBox, gone since 1.21.9: an outline of the box in ARGB. */
    public static void lineBox(PoseStack poseStack, VertexConsumer lines, AABB box, int color) {
        ShapeRenderer.renderShape(poseStack, lines, Shapes.create(box), 0, 0, 0, color, 2.0F);
    }
}
