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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import org.jetbrains.annotations.Nullable;
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

    // model data worked out against the preview's blocks, redone when they change (generation)
    private ModelData cachedModelData = ModelData.EMPTY;
    private int cachedGeneration = -1;

    public void render(GuiGraphics gfx, int mouseX, int mouseY, AutoTransform mouseTransform) {
        render(gfx, mouseX, mouseY, mouseTransform, null, 0);
    }

    /**
     * @param world      the preview's blocks, so connected-texture models can see their neighbours; null for none
     * @param generation changes whenever the preview's blocks change
     */
    public void render(GuiGraphics gfx, int mouseX, int mouseY, AutoTransform mouseTransform, @Nullable BlockAndTintGetter world, int generation) {
        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.mulPoseMatrix(mouseTransform.getModelTransform());
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = gfx.bufferSource();
        var model = brd.getBlockModel(state);
        var modeldata = be != null ? be.getModelData() : ModelData.EMPTY;
        if (world != null) {
            if (cachedGeneration != generation) {
                try {
                    cachedModelData = model.getModelData(world, pos, state, modeldata);
                } catch (RuntimeException e) {
                    // a model that can't handle the preview world keeps its plain look
                    cachedModelData = modeldata;
                }
                cachedGeneration = generation;
            }
            modeldata = cachedModelData;
        }
        var layers = model.getRenderTypes(state, randomSource, modeldata);
        for (RenderType layer : layers) {
            brd.renderSingleBlock(state, pose, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, modeldata, layer);
        }

        if (ber != null) {
            try {
                ber.render(be, 1.f, gfx.pose(), bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {

            }
        }
        // no endBatch() here: the structure renderer flushes once per frame after all blocks
        pose.popPose();
    }

    /**
     * True when this block is a full opaque cube with nothing drawn by a block entity renderer,
     * so a neighbour fully surrounded by such blocks can never be seen.
     */
    public BlockState getState() {
        return state;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return be;
    }

    public boolean isOpaqueCube() {
        return ber == null && state != null && state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
