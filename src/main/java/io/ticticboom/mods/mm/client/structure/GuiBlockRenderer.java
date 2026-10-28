package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiBlockRenderer {
    private final Block block;
    private BlockEntityRenderer<BlockEntity> ber;
    private BlockEntity be;
    private BlockState state;
    private final List<StructurePieceModifier> modifiers;
    private final BlockState fixedState;
    private static Minecraft mc = Minecraft.getInstance();
    private BlockPos pos;

    public GuiBlockRenderer(Block block, List<StructurePieceModifier> modifiers) {
        this.block = block;
        this.modifiers = modifiers;
        this.fixedState = null;
    }

    public GuiBlockRenderer(BlockState state) {
        this.block = state.getBlock();
        this.modifiers = List.of();
        this.fixedState = state;
    }

    public void setupAt(BlockPos pos) {
        this.pos = pos;
        blockEntityModelData = null;
        state = fixedState != null ? fixedState : block.defaultBlockState();
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

    private ModelData cachedModelData = ModelData.EMPTY;
    private int cachedGeneration = -1;
    private boolean modelBroken = false;
    private ModelData blockEntityModelData;

    private ModelData blockEntityModelData() {
        if (blockEntityModelData == null) {
            try {
                blockEntityModelData = be != null ? be.getModelData() : ModelData.EMPTY;
            } catch (RuntimeException e) {
                blockEntityModelData = ModelData.EMPTY;
            }
        }
        return blockEntityModelData;
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, AutoTransform mouseTransform) {
        extractRenderState(gfx, mouseX, mouseY, mouseTransform, null, 0);
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, AutoTransform mouseTransform, @Nullable BlockAndTintGetter world, int generation) {
        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.mulPose(mouseTransform.getModelTransform());
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = gfx.bufferSource();
        var model = brd.getBlockModel(state);
        var modeldata = blockEntityModelData();
        if (world != null) {
            if (cachedGeneration != generation) {
                try {
                    cachedModelData = model.getModelData(world, pos, state, modeldata);
                } catch (RuntimeException e) {
                    cachedModelData = modeldata;
                }
                cachedGeneration = generation;
            }
            modeldata = cachedModelData;
        }
        if (!modelBroken) {
            try {
                renderModel(brd, pose, bufferSource, model, modeldata);
            } catch (RuntimeException e) {
                try {
                    renderModel(brd, pose, bufferSource, model, ModelData.EMPTY);
                } catch (RuntimeException e2) {
                    modelBroken = true;
                }
            }
        }

        if (ber != null) {
            try {
                ber.render(be, 1.f, gfx.pose(), bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {

            }
        }
        pose.popPose();
    }

    private void renderModel(BlockRenderDispatcher brd, PoseStack pose, MultiBufferSource bufferSource, BakedModel model, ModelData modeldata) {
        for (RenderType layer : model.getRenderTypes(state, randomSource, modeldata)) {
            brd.renderSingleBlock(state, pose, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, modeldata, layer);
        }
    }

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
