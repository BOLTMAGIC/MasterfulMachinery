package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.event.ControllerColorEvents;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PortStatusLightRenderer implements BlockEntityRenderer<BlockEntity, PortStatusLightRenderer.State> {
    private static final Identifier TEXTURE = Ref.id("textures/block/base_ports/port_light.png");
    private static final float OUT = 0.002f;
    private static final float[][] UV = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};

    public static class State extends BlockEntityRenderState {
        boolean visible;
        int color;
        final boolean[] faces = new boolean[6];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.visible = be instanceof AbstractPortBlockEntity && MMConfigSetup.CLIENT.portStatusLight.get();
        if (!(be instanceof AbstractPortBlockEntity port) || !state.visible) {
            return;
        }
        var machineState = port.getMachineState();
        int own = port.getMachineColor(machineState);
        state.color = own >= 0 ? own : ControllerColorEvents.configColor(machineState);
        var blockState = be.getBlockState();
        for (Direction face : Direction.values()) {
            state.faces[face.ordinal()] = be.getLevel() == null
                    || Block.shouldRenderFace(be.getLevel(), be.getBlockPos(), blockState, be.getLevel().getBlockState(be.getBlockPos().relative(face)), face);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) {
            return;
        }
        float r = ((state.color >> 16) & 0xFF) / 255f;
        float g = ((state.color >> 8) & 0xFF) / 255f;
        float b = (state.color & 0xFF) / 255f;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (last, vc) -> {
            for (Direction face : Direction.values()) {
                if (!state.faces[face.ordinal()]) {
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
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(last, face.getStepX(), face.getStepY(), face.getStepZ());
                }
            }
        });
    }
}
