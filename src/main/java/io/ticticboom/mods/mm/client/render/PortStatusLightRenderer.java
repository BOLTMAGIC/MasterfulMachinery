package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMClientConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the port's status light: a small lamp in the middle of each face, in the color of its machine's state.
 * Drawn here instead of in the block model so it shows even when a pack replaces the port models.
 */
public class PortStatusLightRenderer implements BlockEntityRenderer<BlockEntity> {
    private static final ResourceLocation TEXTURE = Ref.id("textures/block/base_ports/port_light.png");
    // just outside the block so the lamp doesn't fight with the port's own faces
    private static final float OUT = 0.002f;
    // close to the port's base texture, so a disabled status light blends in
    private static final int OFF_COLOR = 0x5E5E5E;
    private static final int FALLBACK_COLOR = 0x5CFF89;

    @Override
    public void render(BlockEntity port, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = port.getBlockState();
        if (!state.hasProperty(ControllerState.PROPERTY) || !MMConfigSetup.CLIENT.portStatusLight.get()) {
            return;
        }
        int color = color(port, state.getValue(ControllerState.PROPERTY));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (Direction face : Direction.values()) {
            // skip faces covered by a full block, as the block model would
            if (port.getLevel() != null && !Block.shouldRenderFace(state, port.getLevel(), port.getBlockPos(), face, port.getBlockPos().relative(face))) {
                continue;
            }
            quad(vc, m, n, face, r, g, b);
        }
    }

    private static int color(BlockEntity port, ControllerState state) {
        // the machine's own colors, sent by its controller (not every port type keeps them)
        int own = port instanceof AbstractPortBlockEntity p ? p.getMachineColor(state.ordinal()) : -1;
        if (own >= 0) {
            return own;
        }
        var config = MMConfigSetup.CLIENT;
        var value = switch (state) {
            case UNFORMED -> config.controllerUnformedColor;
            case IDLE -> config.controllerIdleColor;
            case WORKING -> config.controllerWorkingColor;
        };
        Integer parsed = MMClientConfig.parseColor(value.get());
        return parsed != null ? parsed : FALLBACK_COLOR;
    }

    /** A full face quad textured with the lamp texture (only its middle pixels are opaque), lit at full brightness. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, Direction face, float r, float g, float b) {
        float[][] corners = switch (face) {
            case NORTH -> new float[][]{{1, 0, -OUT}, {0, 0, -OUT}, {0, 1, -OUT}, {1, 1, -OUT}};
            case SOUTH -> new float[][]{{0, 0, 1 + OUT}, {1, 0, 1 + OUT}, {1, 1, 1 + OUT}, {0, 1, 1 + OUT}};
            case WEST -> new float[][]{{-OUT, 0, 0}, {-OUT, 0, 1}, {-OUT, 1, 1}, {-OUT, 1, 0}};
            case EAST -> new float[][]{{1 + OUT, 0, 1}, {1 + OUT, 0, 0}, {1 + OUT, 1, 0}, {1 + OUT, 1, 1}};
            case DOWN -> new float[][]{{0, -OUT, 1}, {0, -OUT, 0}, {1, -OUT, 0}, {1, -OUT, 1}};
            case UP -> new float[][]{{0, 1 + OUT, 0}, {0, 1 + OUT, 1}, {1, 1 + OUT, 1}, {1, 1 + OUT, 0}};
        };
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, corners[i][0], corners[i][1], corners[i][2])
                    .color(r, g, b, 1f)
                    .uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(n, face.getStepX(), face.getStepY(), face.getStepZ())
                    .endVertex();
        }
    }
}
