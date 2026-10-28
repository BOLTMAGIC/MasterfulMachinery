package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.ticticboom.mods.mm.client.FluidSprites;
import io.ticticboom.mods.mm.client.geo.GeoBridge;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.controller.machine.register.ModelDisplay;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.attachment.visuals.FluidVolumeState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a formed machine's fluid volumes and its GeckoLib model. Since 1.21.9 block entity renderers extract a render
 * state and submit geometry later in the frame; the state keeps the controller, read on the render thread.
 */
public class ControllerVisualsRenderer implements BlockEntityRenderer<MachineControllerBlockEntity, ControllerVisualsRenderer.State> {
    private static final int MIN_ALPHA = 0xA0;

    public static class State extends BlockEntityRenderState {
        @Nullable
        MachineControllerBlockEntity be;
        float partialTick;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MachineControllerBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.be = be.getLevel() == null ? null : be;
        state.partialTick = partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var be = state.be;
        if (be == null) {
            return;
        }
        for (FluidVolumeState volume : be.getFluidVolumes()) {
            if (!volume.getFluid().isEmpty() && volume.getFill() > 0 && volume.getBounds() != null) {
                renderFluidVolume(be, volume, pose, collector);
            }
        }
        renderGeoModel(be, pose, collector, camera, state.lightCoords, state.partialTick);
    }

    private void renderGeoModel(MachineControllerBlockEntity be, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera,
                                int light, float partialTick) {
        var blockState = be.getBlockState();
        if (!blockState.hasProperty(ControllerState.PROPERTY) || !blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        ControllerState state = blockState.getValue(ControllerState.PROPERTY);
        if (state == ControllerState.UNFORMED) {
            return;
        }
        GeoModelConfig config = be.geoModel();
        GeoBridge bridge = GeoBridge.get();
        // the controller screen can hide the whole model
        if (config == null || bridge == null || be.getModelDisplay() == ModelDisplay.HIDDEN) {
            return;
        }
        Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        double[] offset = config.worldOffset(facing);
        int modelLight = light;
        if (config.glow()) {
            modelLight = LightCoordsUtil.FULL_BRIGHT;
        } else {
            var at = BlockPos.containing(be.getBlockPos().getX() + 0.5 + offset[0], be.getBlockPos().getY() + offset[1], be.getBlockPos().getZ() + 0.5 + offset[2]);
            modelLight = LevelRenderer.getLightCoords(be.getLevel(), at);
        }
        pose.pushPose();
        pose.translate(0.5 + offset[0], offset[1], 0.5 + offset[2]);
        pose.mulPose(Axis.YP.rotationDegrees(config.modelYRot(facing)));
        pose.scale(config.scale(), config.scale(), config.scale());
        pose.translate(-0.5, -0.51, -0.5);
        bridge.render(be, config, state, pose, collector, camera, modelLight, partialTick);
        pose.popPose();
    }

    private void renderFluidVolume(MachineControllerBlockEntity be, FluidVolumeState volume, PoseStack pose, SubmitNodeCollector collector) {
        var fluid = volume.getFluid();
        TextureAtlasSprite sprite = FluidSprites.still(fluid);

        var center = volume.getBounds().getCenter();
        var centerPos = be.getBlockPos().offset((int) Math.floor(center.x), (int) Math.floor(center.y), (int) Math.floor(center.z));
        int tint = FluidSprites.tint(fluid);
        int color = Math.max(tint >>> 24, MIN_ALPHA) << 24 | (tint & 0xFFFFFF);
        int packed = LevelRenderer.getLightCoords(be.getLevel(), centerPos);
        int fluidLight = fluid.getFluid().getFluidType().getLightLevel(fluid);
        int light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(packed), fluidLight), LightCoordsUtil.sky(packed));

        var cells = volume.getCellSet();
        boolean gas = volume.isGas();
        float level = volume.getLevel();
        collector.submitCustomGeometry(pose, RenderTypes.translucentMovingBlock(), (last, vc) -> {
        for (long cell : volume.getCells()) {
            int cx = BlockPos.getX(cell), cy = BlockPos.getY(cell), cz = BlockPos.getZ(cell);
            float lo = gas ? Math.max(cy, level) : cy;
            float hi = gas ? cy + 1 : Math.min(cy + 1, level);
            if (lo >= hi) {
                continue;
            }
            boolean west = cells.contains(BlockPos.offset(cell, -1, 0, 0));
            boolean east = cells.contains(BlockPos.offset(cell, 1, 0, 0));
            boolean north = cells.contains(BlockPos.offset(cell, 0, 0, -1));
            boolean south = cells.contains(BlockPos.offset(cell, 0, 0, 1));
            boolean below = cells.contains(BlockPos.offset(cell, 0, -1, 0));
            boolean above = cells.contains(BlockPos.offset(cell, 0, 1, 0));
            boolean openTop = gas ? !above : !(above && level > cy + 1);
            boolean openBottom = gas ? !(below && level < cy) : !below;
            float ax = cx, bx = cx + 1, az = cz, bz = cz + 1, ay = lo, by = hi;
            if (openTop) {
                face(vc, last, sprite, color, light, Direction.UP, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (openBottom) {
                face(vc, last, sprite, color, light, Direction.DOWN, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!west) {
                face(vc, last, sprite, color, light, Direction.WEST, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!east) {
                face(vc, last, sprite, color, light, Direction.EAST, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!north) {
                face(vc, last, sprite, color, light, Direction.NORTH, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!south) {
                face(vc, last, sprite, color, light, Direction.SOUTH, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
        }
        });
    }

    private static void face(VertexConsumer vc, PoseStack.Pose last, TextureAtlasSprite sprite, int color, int light, Direction face,
                             int cx, int cy, int cz, float ax, float ay, float az, float bx, float by, float bz) {
        float[][] corners = switch (face) {
            case UP -> new float[][]{{ax, by, az}, {ax, by, bz}, {bx, by, bz}, {bx, by, az}};
            case DOWN -> new float[][]{{ax, ay, bz}, {ax, ay, az}, {bx, ay, az}, {bx, ay, bz}};
            case NORTH -> new float[][]{{bx, by, az}, {bx, ay, az}, {ax, ay, az}, {ax, by, az}};
            case SOUTH -> new float[][]{{ax, by, bz}, {ax, ay, bz}, {bx, ay, bz}, {bx, by, bz}};
            case WEST -> new float[][]{{ax, by, az}, {ax, ay, az}, {ax, ay, bz}, {ax, by, bz}};
            case EAST -> new float[][]{{bx, by, bz}, {bx, ay, bz}, {bx, ay, az}, {bx, by, az}};
        };
        for (float[] c : corners) {
            float u;
            float v;
            switch (face.getAxis()) {
                case Y -> {
                    u = c[0] - cx;
                    v = c[2] - cz;
                }
                case Z -> {
                    u = c[0] - cx;
                    v = 1 - (c[1] - cy);
                }
                default -> {
                    u = c[2] - cz;
                    v = 1 - (c[1] - cy);
                }
            }
            vc.addVertex(last, c[0], c[1], c[2])
                    .setColor(color)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setLight(light)
                    .setNormal(last, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // fluid volumes and models reach outside the controller block
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(MachineControllerBlockEntity be) {
        if (be.geoModel() != null) {
            return AABB.INFINITE;
        }
        var bounds = be.getVisualBounds();
        return bounds != null ? bounds : BlockEntityRenderer.super.getRenderBoundingBox(be);
    }
}
