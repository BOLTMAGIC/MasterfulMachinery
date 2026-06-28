package io.ticticboom.mods.mm.client.blueprint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.item.BlueprintItem;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-only blueprint placement preview.
 *
 * This is deliberately tiny and isolated: it only renders line boxes during the
 * normal block-highlight event and does not change structure validation, placement,
 * packets, screens, or world state. Reverting the feature is just deleting this file
 * and the optional tooltip line in {@link BlueprintItem}.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlueprintPastePreviewRenderer {
    private BlueprintPastePreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        if (!player.getAbilities().instabuild || !player.isShiftKeyDown()) {
            return;
        }

        StructureModel structure = getHeldBlueprintStructure(player);
        if (structure == null) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        Rotation rotation = rotationFromPlayer(player);
        StructurePasteUtil.PastePlan pastePlan = StructurePasteUtil.createPlanForPlacementAnchor(structure, hit.getBlockPos().relative(hit.getDirection()), rotation, player.getDirection());
        List<StructurePasteUtil.PlannedBlock> plan = pastePlan.blocks();
        if (plan.isEmpty()) {
            return;
        }

        renderPlan(event, level, plan);
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

    private static Rotation rotationFromPlayer(Player player) {
        return switch (player.getDirection()) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static void renderPlan(RenderHighlightEvent.Block event, Level level, List<StructurePasteUtil.PlannedBlock> plan) {
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();

        boolean valid = true;
        for (StructurePasteUtil.PlannedBlock planned : plan) {
            if (!StructurePasteUtil.canPlaceAt(level, planned)) {
                valid = false;
                break;
            }
        }

        // Draw only one outer outline.
        AABB fullBounds = StructurePasteUtil.bounds(plan).inflate(0.02D).move(-camera.x, -camera.y, -camera.z);
        if (valid) {
            LevelRenderer.renderLineBox(poseStack, buffer, fullBounds, 0.0F, 1.0F, 0.05F, 1.0F);
        } else {
            LevelRenderer.renderLineBox(poseStack, buffer, fullBounds, 1.0F, 0.0F, 0.0F, 1.0F);
        }
    }
}
