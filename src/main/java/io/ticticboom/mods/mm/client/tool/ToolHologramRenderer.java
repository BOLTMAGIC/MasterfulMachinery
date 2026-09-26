package io.ticticboom.mods.mm.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.ToolBuildPlan;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Client-only preview of what the multiblock tool would build where the crosshair is: translucent ghost blocks, red
 * boxes on obstructed positions and one outline around it all (green, red when anything is in the way). Also
 * outlines a machine about to be dismantled in red. The plan comes from the same {@link ToolBuildPlan#create} the
 * server uses; it is rebuilt on the client tick only when its inputs change (or every {@link #REFRESH_TICKS}), never
 * per frame.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ToolHologramRenderer {
    private static final float GHOST_ALPHA = 0.4F;
    /** How often an unchanged plan is rebuilt anyway, to follow blocks placed or broken around it. */
    private static final int REFRESH_TICKS = 10;

    // inputs of the cached plan
    private static BlockPos keyPos;
    private static Direction keyFace;
    private static Direction keyFacing;
    private static int keyTurns;
    private static StructureModel keyStructure;
    private static long keyBucket;
    // the cached plan: ghosts to draw, obstructed positions, the whole outline (null = nothing to show)
    private static List<AssemblyPlanner.Planned> ghosts = List.of();
    private static List<BlockPos> obstructed = List.of();
    private static AABB bounds;

    private ToolHologramRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            clear();
            return;
        }
        ItemStack tool = ToolKeys.heldTool(player);
        StructureModel structure = tool.isEmpty() ? null : ToolBuilds.selectedStructure(tool);
        if (structure == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || (player.isShiftKeyDown() && !ToolKeys.recentlyRotated(level))
                || !ToolKeys.dismantleHighlight(level).isEmpty()
                || level.getBlockEntity(hit.getBlockPos()) instanceof MachineControllerBlockEntity) {
            // a controller is completed in place (or opened), not built next to
            clear();
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        Direction facing = player.getDirection();
        int turns = ToolData.extraTurns(tool);
        long bucket = level.getGameTime() / REFRESH_TICKS;
        if (bounds != null && pos.equals(keyPos) && face == keyFace && facing == keyFacing && turns == keyTurns
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

    private static void rebuild(Level level, Player player, ItemStack tool, StructureModel structure) {
        ToolBuildPlan plan = ToolBuildPlan.create(level, structure, keyPos, keyFace, keyFacing, keyTurns, ToolData.tiers(tool), availableSnapshot(player, tool));
        if (plan == null || plan.plan().steps().isEmpty()) {
            clear();
            return;
        }
        Set<BlockPos> blocked = new HashSet<>(plan.obstructed());
        var toDraw = new ArrayList<AssemblyPlanner.Planned>();
        AABB box = null;
        for (AssemblyPlanner.Planned step : plan.plan().steps()) {
            AABB cell = new AABB(step.pos());
            box = box == null ? cell : box.minmax(cell);
            BlockState existing = level.getBlockState(step.pos());
            // blocks already in place need no ghost
            if (!blocked.contains(step.pos()) && !existing.is(step.state().getBlock()) && !step.accepted().contains(existing.getBlock())) {
                toDraw.add(step);
            }
        }
        ghosts = toDraw;
        obstructed = plan.obstructed();
        bounds = box;
    }

    /**
     * What the tool's store and the player's inventory hold, read once per rebuild (the build's own source would
     * re-read the whole store for every candidate). Everything in creative.
     */
    private static Predicate<Block> availableSnapshot(Player player, ItemStack tool) {
        if (player.getAbilities().instabuild) {
            return block -> true;
        }
        Set<Item> items = new HashSet<>();
        ToolStore store = new ToolStore(tool);
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack stack = store.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        return block -> items.contains(block.asItem());
    }

    private static void clear() {
        ghosts = List.of();
        obstructed = List.of();
        bounds = null;
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
        if (ghosts.isEmpty()) {
            return;
        }
        VertexConsumer ghost = new AlphaVertexConsumer(buffers.getBuffer(RenderType.translucent()), GHOST_ALPHA);
        for (AssemblyPlanner.Planned step : ghosts) {
            BlockPos pos = step.pos();
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            // slightly shrunk so faces never z-fight with neighbouring real blocks
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.scale(0.98F, 0.98F, 0.98F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            BlockState state = step.state();
            // null render type: every quad of the model, whatever layer it normally draws in
            dispatcher.getModelRenderer().renderModel(poseStack.last(), ghost, state, dispatcher.getBlockModel(state),
                    1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poseStack.popPose();
        }
        buffers.endBatch(RenderType.translucent());
    }

    /** Passes vertices through with their alpha replaced, making any block model see-through. */
    private record AlphaVertexConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, (int) (alpha * 255));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int light, float nx, float ny, float nz) {
            delegate.vertex(x, y, z, r, g, b, alpha, u, v, overlay, light, nx, ny, nz);
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, (int) (alpha * 255));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
