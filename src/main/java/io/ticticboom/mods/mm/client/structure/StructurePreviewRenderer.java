package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.render.BlockModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

/** Draws a StructurePreviewRenderState: the blocks, lit like items, through an orthographic camera. */
@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class StructurePreviewRenderer extends PictureInPictureRenderer<StructurePreviewRenderState> {

    public StructurePreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @SubscribeEvent
    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(StructurePreviewRenderState.class, StructurePreviewRenderer::new);
    }

    @Override
    public Class<StructurePreviewRenderState> getRenderStateClass() {
        return StructurePreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(StructurePreviewRenderState state, PoseStack pose) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        // the texture's y runs down and its z away from the viewer
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(state.transform());
        var type = RenderTypes.translucentMovingBlock();
        var consumer = this.bufferSource.getBuffer(type);
        for (StructurePreviewRenderState.Block block : state.blocks()) {
            pose.pushPose();
            pose.translate(block.pos().getX(), block.pos().getY(), block.pos().getZ());
            try {
                BlockModels.draw(pose.last(), consumer, state.level(), block.pos(), block.state(), -1, LightCoordsUtil.FULL_BRIGHT);
            } catch (RuntimeException e) {
                // a model that can't be drawn outside a real level is left out of the preview
            }
            pose.popPose();
        }
        this.bufferSource.endBatch(type);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "mm structure preview";
    }
}
