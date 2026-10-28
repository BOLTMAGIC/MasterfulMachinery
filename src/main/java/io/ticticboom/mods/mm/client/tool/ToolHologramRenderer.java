package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.client.structure.GhostBlocks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.FixedBuildPlan;
import io.ticticboom.mods.mm.tool.ToolBuildPlan;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ToolHologramRenderer {
    private static final int REFRESH_TICKS = 10;

    private static BlockPos keyPos;
    private static Direction keyFace;
    private static Direction keyFacing;
    private static int keyTurns;
    private static Object keyStructure;
    private static long keyBucket;
    private static boolean planCached;
    private static List<AssemblyPlanner.Planned> ghosts = List.of();
    private static List<BlockPos> obstructed = List.of();
    private static AABB bounds;

    private ToolHologramRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            clear();
            return;
        }
        ItemStack tool = ToolKeys.heldTool(player);
        Object structure = tool.isEmpty() ? null : ToolBuilds.selectedStructure(tool);
        if (structure == null && !tool.isEmpty()) {
            ResourceLocation builderId = ToolData.builderStructure(tool);
            structure = builderId == null ? null : BuildableStructureRegistry.CLIENT.get(builderId);
        }
        ToolTarget target = structure == null ? null : ToolTarget.resolve(player, tool, ToolPlacementSettings.range());
        if (target == null
                || !ToolKeys.dismantleHighlight(level).isEmpty()
                || (ToolData.anchor(tool) == null && structure instanceof StructureModel
                    && level.getBlockEntity(target.pos()) instanceof MachineControllerBlockEntity)) {
            // a controller is completed in place (or opened), not built next to
            clear();
            return;
        }
        BlockPos pos = target.pos();
        Direction face = target.face();
        Direction facing = target.facing();
        int turns = ToolData.extraTurns(tool);
        long bucket = level.getGameTime() / REFRESH_TICKS;
        if (planCached && pos.equals(keyPos) && face == keyFace && facing == keyFacing && turns == keyTurns
                && structure == keyStructure && bucket == keyBucket) {
            return;
        }
        keyPos = pos.immutable();
        keyFace = face;
        keyFacing = facing;
        keyTurns = turns;
        keyStructure = structure;
        keyBucket = bucket;
        rebuild(level, player, tool, structure);
    }

    private static void rebuild(Level level, Player player, ItemStack tool, Object structure) {
        AssemblyPlanner.Plan steps;
        List<BlockPos> planObstructed;
        if (structure instanceof BuildableStructure builder) {
            FixedBuildPlan plan = FixedBuildPlan.create(level, builder, keyPos, keyFace, keyFacing, keyTurns);
            steps = plan.plan();
            planObstructed = plan.obstructed();
        } else {
            ToolBuildPlan plan = ToolBuildPlan.create(level, (StructureModel) structure, keyPos, keyFace, keyFacing, keyTurns,
                    ToolData.tiers(tool), ToolBuildPlan.availableSnapshot(player, tool));
            if (plan == null) {
                clear();
                return;
            }
            steps = plan.plan();
            planObstructed = plan.obstructed();
        }
        if (steps.steps().isEmpty()) {
            clear();
            return;
        }
        Set<BlockPos> blocked = new HashSet<>(planObstructed);
        var toDraw = new ArrayList<AssemblyPlanner.Planned>();
        AABB box = null;
        for (AssemblyPlanner.Planned step : steps.steps()) {
            AABB cell = new AABB(step.pos());
            box = box == null ? cell : box.minmax(cell);
            if (!ToolTarget.usablePosition(level, step.pos())) continue;
            BlockState existing = level.getBlockState(step.pos());
            if (!blocked.contains(step.pos()) && !existing.is(step.state().getBlock()) && !step.accepted().contains(existing.getBlock())) {
                toDraw.add(step);
            }
        }
        ghosts = toDraw;
        obstructed = planObstructed;
        bounds = toDraw.isEmpty() && planObstructed.isEmpty() ? null : box;
        planCached = true;
    }

    private static void clear() {
        ghosts = List.of();
        obstructed = List.of();
        bounds = null;
        planCached = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        List<BlockPos> dismantle = ToolKeys.dismantleHighlight(level);
        if (bounds == null && dismantle.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if (bounds != null) {
            renderGhosts(poseStack, buffers, mc.getBlockRenderer());
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        if (bounds != null) {
            for (BlockPos pos : obstructed) {
                LevelRenderer.renderLineBox(poseStack, lines, new AABB(pos).inflate(0.01D), 1.0F, 0.0F, 0.0F, 1.0F);
            }
            boolean clear = obstructed.isEmpty();
            LevelRenderer.renderLineBox(poseStack, lines, bounds.inflate(0.02D), clear ? 0.0F : 1.0F, clear ? 1.0F : 0.0F, clear ? 0.05F : 0.0F, 1.0F);
        }
        for (BlockPos pos : dismantle) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(pos).inflate(0.01D), 1.0F, 0.1F, 0.1F, 1.0F);
        }
        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void renderGhosts(PoseStack poseStack, MultiBufferSource.BufferSource buffers, BlockRenderDispatcher dispatcher) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (AssemblyPlanner.Planned step : ghosts) {
            blocks.put(step.pos(), step.state());
        }
        GhostBlocks.render(poseStack, buffers, dispatcher, blocks);
    }
}
