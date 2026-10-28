package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.event.ControllerColorEvents;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PortStatusLightRenderer implements BlockEntityRenderer<BlockEntity> {
    private static final Identifier TEXTURE = Ref.id("textures/block/base_ports/port_light.png");
    private static final float OUT = 0.002f;
    private static final float[][] UV = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(be instanceof AbstractPortBlockEntity port) || !MMConfigSetup.CLIENT.portStatusLight.get()) {
            return;
        }
        var machineState = port.getMachineState();
        int own = port.getMachineColor(machineState);
        int color = own >= 0 ? own : ControllerColorEvents.configColor(machineState);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var last = pose.last();
        var state = be.getBlockState();
        for (Direction face : Direction.values()) {
            if (be.getLevel() != null && !Block.shouldRenderFace(state, be.getLevel(), be.getBlockPos(), face, be.getBlockPos().relative(face))) {
                continue;
            }
            float[][] corners = switch (face) {
                case NORTH -> new float[][]{{1, 0, -OUT}, {0, 0, -OUT}, {0, 1, -OUT}, {1, 1, -OUT}};
                case SOUTH -> new float[][]{{0, 0, 1 + OUT}, {1, 0, 1 + OUT}, {1, 1, 1 + OUT}, {0, 1, 1 + OUT}};
                case WEST -> new float[][]{{-OUT, 0, 0}, {-OUT, 0, 1}, {-OUT, 1, 1}, {-OUT, 1, 0}};
                case EAST -> new float[][]{{1 + OUT, 0, 1}, {1 + OUT, 0, 0}, {1 + OUT, 1, 0}, {1 + OUT, 1, 1}};
                case DOWN -> new float[][]{{0, -OUT, 1}, {0, -OUT, 0}, {1, -OUT, 0}, {1, -OUT, 1}};
                case UP -> new float[][]{{0, 1 + OUT, 0}, {0, 1 + OUT, 1}, {1, 1 + OUT, 1}, {1, 1 + OUT, 0}};
            };
            for (int i = 0; i < 4; i++) {
                vc.addVertex(last, corners[i][0], corners[i][1], corners[i][2])
                        .setColor(r, g, b, 1f)
                        .setUv(UV[i][0], UV[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(last, face.getStepX(), face.getStepY(), face.getStepZ());
            }
        }
    }
}
