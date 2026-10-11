package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.ticticboom.mods.mm.client.geo.GeoBridge;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Draws the machine's GeckoLib model (geoModel) while it's formed. Does nothing without GeckoLib or a geoModel.
 * Registered for every controller block entity type; skips any that isn't a MachineControllerBlockEntity.
 */
public class ControllerGeoRenderer implements BlockEntityRenderer<BlockEntity> {

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(blockEntity instanceof MachineControllerBlockEntity be)) {
            return;
        }
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
        if (config == null || bridge == null) {
            return;
        }
        Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        double[] offset = config.worldOffset(facing);
        int modelLight = config.glow() ? LightTexture.FULL_BRIGHT : lightAt(be, offset, light);

        pose.pushPose();
        pose.translate(0.5 + offset[0], offset[1], 0.5 + offset[2]);
        pose.mulPose(Axis.YP.rotationDegrees(config.modelYRot(facing)));
        pose.scale(config.scale(), config.scale(), config.scale());
        // GeckoLib's object renderer moves the model by (0.5, 0.51, 0.5) itself: undo it so the origin stays here
        pose.translate(-0.5, -0.51, -0.5);
        bridge.render(be, config, state, pose, buffers, modelLight);
        pose.popPose();
    }

    /** The light where the model is, not at the controller (which may sit in the structure's dark wall). */
    private static int lightAt(MachineControllerBlockEntity be, double[] offset, int fallback) {
        if (be.getLevel() == null) {
            return fallback;
        }
        BlockPos at = BlockPos.containing(be.getBlockPos().getX() + 0.5 + offset[0], be.getBlockPos().getY() + offset[1],
                be.getBlockPos().getZ() + 0.5 + offset[2]);
        return LevelRenderer.getLightColor(be.getLevel(), at);
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return blockEntity instanceof MachineControllerBlockEntity be && be.geoModel() != null;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
