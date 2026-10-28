package io.ticticboom.mods.mm.client.blueprint;

import net.minecraft.util.ARGB;
import io.ticticboom.mods.mm.client.render.BlockModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.structure.GhostBlocks;
import io.ticticboom.mods.mm.item.BlueprintItem;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class BlueprintPastePreviewRenderer {
    private static final int REFRESH_TICKS = 10;

    private static BlockPos keyAnchor;
    private static Direction keyFacing;
    private static StructureModel keyStructure;
    private static long keyBucket;
    private static Map<BlockPos, BlockState> ghosts = Map.of();
    private static List<BlockPos> obstructed = List.of();
    private static AABB bounds;

    private BlueprintPastePreviewRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || !player.getAbilities().instabuild || !player.isShiftKeyDown()) {
            clear();
            return;
        }
        StructureModel structure = getHeldBlueprintStructure(player);
        if (structure == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }
        BlockPos anchor = hit.getBlockPos().relative(hit.getDirection());
        Direction facing = player.getDirection();
        long bucket = level.getGameTime() / REFRESH_TICKS;
        if (bounds != null && anchor.equals(keyAnchor) && facing == keyFacing && structure == keyStructure && bucket == keyBucket) {
            return;
        }
        keyAnchor = anchor.immutable();
        keyFacing = facing;
        keyStructure = structure;
        keyBucket = bucket;
        rebuild(level, structure, anchor, facing);
    }

    private static void rebuild(Level level, StructureModel structure, BlockPos anchor, Direction facing) {
        List<StructurePasteUtil.PlannedBlock> plan = StructurePasteUtil
                .createPlanForPlacementAnchor(structure, anchor, rotationFrom(facing), facing).blocks();
        if (plan.isEmpty()) {
            clear();
            return;
        }
        Map<BlockPos, BlockState> toDraw = new LinkedHashMap<>();
        List<BlockPos> blocked = new ArrayList<>();
        for (StructurePasteUtil.PlannedBlock planned : plan) {
            if (!StructurePasteUtil.canPlaceAt(level, planned)) {
                blocked.add(planned.pos());
            } else if (!level.getBlockState(planned.pos()).is(planned.state().getBlock())) {
                toDraw.put(planned.pos(), planned.state());
            }
        }
        ghosts = toDraw;
        obstructed = blocked;
        bounds = StructurePasteUtil.bounds(plan);
    }

    private static void clear() {
        ghosts = Map.of();
        obstructed = List.of();
        bounds = null;
        keyStructure = null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (bounds == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        GhostBlocks.render(poseStack, buffers, ghosts);
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        for (BlockPos pos : obstructed) {
            BlockModels.lineBox(poseStack, lines, new AABB(pos).inflate(0.01D), ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F));
        }
        boolean clear = obstructed.isEmpty();
        BlockModels.lineBox(poseStack, lines, bounds.inflate(0.02D), ARGB.colorFromFloat(1.0F, clear ? 0.0F : 1.0F, clear ? 1.0F : 0.0F, clear ? 0.05F : 0.0F));
        buffers.endBatch(RenderTypes.lines());
        poseStack.popPose();
    }

    private static StructureModel getHeldBlueprintStructure(Player player) {
        StructureModel main = getBlueprintStructure(player.getMainHandItem());
        if (main != null) {
            return main;
        }
        return getBlueprintStructure(player.getOffhandItem());
    }

    private static StructureModel getBlueprintStructure(ItemStack stack) {
        if (!(stack.getItem() instanceof BlueprintItem)) {
            return null;
        }
        return BlueprintItem.getStructure(stack);
    }

    private static Rotation rotationFrom(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
