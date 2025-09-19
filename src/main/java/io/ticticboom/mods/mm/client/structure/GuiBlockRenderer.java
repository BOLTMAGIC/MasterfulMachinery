package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiBlockRenderer {
    private final Block block;
    private BlockEntityRenderer<BlockEntity> ber;
    private BlockEntity be;
    private BlockState state;
    private final List<StructurePieceModifier> modifiers;
    private static Minecraft mc = Minecraft.getInstance();
    private BlockPos pos;

    public GuiBlockRenderer(Block block, List<StructurePieceModifier> modifiers) {
        this.block = block;
        this.modifiers = modifiers;
    }

    public void setupAt(BlockPos pos) {
        this.pos = pos;
        state = block.defaultBlockState();
        if (block instanceof EntityBlock eb) {
            be = eb.newBlockEntity(pos, state);
            ber = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        }
        for (StructurePieceModifier mod : modifiers) {
            state = mod.modifyBlockState(state, be, pos);
            if (be != null) {
                be = mod.modifyBlockEntity(state, be, pos);
            }
        }
    }

    private static RandomSource randomSource = RandomSource.create();

    public void render(GuiGraphics gfx, int mouseX, int mouseY, AutoTransform mouseTransform) {
        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        ModelBlockRenderer modelRenderer = brd.getModelRenderer();
        MultiBufferSource.BufferSource bufferSource = gfx.bufferSource();
        var model = brd.getBlockModel(state);
        var modeldata = be != null ? be.getModelData() : ModelData.EMPTY;
        var layers = model.getRenderTypes(state, randomSource, modeldata);
        for (RenderType layer : layers) {
            brd.renderSingleBlock(state, pose, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, modeldata, layer);
        }

        if (ber != null) {
            try {
                ber.render(be, 1.f, gfx.pose(), bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {

            }
        }
        bufferSource.endBatch();
        pose.popPose();
    }
}
