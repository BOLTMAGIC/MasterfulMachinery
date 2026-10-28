package io.ticticboom.mods.mm.client.tool;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.blaze3d.platform.InputConstants;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import net.neoforged.neoforge.network.PacketDistributor;
import io.ticticboom.mods.mm.net.packet.ToolDismantlePkt;
import io.ticticboom.mods.mm.net.packet.ToolRotatePkt;
import io.ticticboom.mods.mm.net.packet.ToolAnchorPkt;
import io.ticticboom.mods.mm.net.packet.ToolBuildPkt;
import io.ticticboom.mods.mm.tool.ToolTarget;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolDismantles;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class ToolKeys {
    /** Key categories are objects since 1.21.9; shown as key.category.mm.main. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Ref.id("main"));
    public static final KeyMapping DISMANTLE = new KeyMapping("key.mm.dismantle", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ANCHOR = new KeyMapping("key.mm.anchor", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    /** How long V must be held: 1 s. */
    public static final int HOLD_TICKS = 20;
    private static final int BAR_SEGMENTS = 10;

    private static BlockPos holdAim;
    private static BlockPos holdController;
    private static List<BlockPos> holdPositions = List.of();
    private static int holdTicks;
    private static boolean holdLatched;
    private static boolean barShown;
    private static boolean noMachineShown;
    private static BlockPos pendingController;
    private static List<BlockPos> pendingPositions = List.of();
    private static long pendingUntil;
    // part of a notch scrolled so far (touchpads send fractions); forgotten after SCROLL_RESET_MS
    private static double scrollAccum;
    private static long lastScrollMs;
    private static final long SCROLL_RESET_MS = 500;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(DISMANTLE);
        event.register(ANCHOR);
    }

    public static ItemStack heldTool(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MultiblockToolItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    static List<BlockPos> dismantleHighlight(Level level) {
        if (holdController != null) {
            return holdPositions;
        }
        if (pendingController != null && level.getGameTime() <= pendingUntil) {
            return pendingPositions;
        }
        return List.of();
    }

    /** Forgets the aimed-at machine and clears the progress bar if it was showing. */
    private static void loseTarget(Player player) {
        if (barShown) {
            player.sendOverlayMessage(Component.empty());
            barShown = false;
        }
        holdAim = null;
        holdController = null;
        holdPositions = List.of();
        holdTicks = 0;
    }

    @EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
    public static final class Input {
        private Input() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            Level level = mc.level;
            if (player == null || level == null) {
                holdAim = null;
                holdController = null;
                holdPositions = List.of();
                holdTicks = 0;
                holdLatched = false;
                barShown = false;
                noMachineShown = false;
                pendingController = null;
                scrollAccum = 0;
                return;
            }
            if (!player.isShiftKeyDown() || heldTool(player).isEmpty()) {
                scrollAccum = 0;
            }
            while (ANCHOR.consumeClick()) {
                if (mc.screen == null && !heldTool(player).isEmpty()) {
                    InteractionHand hand = player.getMainHandItem().getItem() instanceof MultiblockToolItem
                            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    ClientPacketDistributor.sendToServer(new ToolAnchorPkt(hand));
                }
            }
            if (heldTool(player).isEmpty() || mc.screen != null || !DISMANTLE.isDown()) {
                loseTarget(player);
                holdLatched = false;
                noMachineShown = false;
                return;
            }
            if (holdLatched) {
                return;
            }
            BlockPos target = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
            if (target == null) {
                loseTarget(player);
                return;
            }
            if (!target.equals(holdAim)) {
                holdAim = target.immutable();
                List<BlockPos> positions = DismantlePlanner.previewPositions(level, holdAim, heldTool(player));
                BlockPos controller = positions.isEmpty() ? null : positions.get(positions.size() - 1);
                if (!Objects.equals(controller, holdController)) {
                    holdTicks = 0;
                }
                holdController = controller;
                holdPositions = positions;
            }
            if (holdController == null) {
                if (barShown) {
                    player.sendOverlayMessage(Component.empty());
                    barShown = false;
                }
                if (!noMachineShown) {
                    player.sendOverlayMessage(Component.translatable("message.mm.tool.dismantle.no_machine"));
                    noMachineShown = true;
                }
                return;
            }
            noMachineShown = false;
            holdTicks++;
            player.sendOverlayMessage(Component.translatable("message.mm.tool.dismantle.progress", bar(holdTicks)));
            barShown = true;
            if (holdTicks >= HOLD_TICKS) {
                ClientPacketDistributor.sendToServer(new ToolDismantlePkt(holdAim));
                holdLatched = true;
                barShown = false;
                holdAim = null;
                holdController = null;
                holdPositions = List.of();
                holdTicks = 0;
            }
        }

        private static String bar(int ticks) {
            int filled = Math.min(BAR_SEGMENTS, ticks * BAR_SEGMENTS / HOLD_TICKS);
            return "▓".repeat(filled) + "░".repeat(BAR_SEGMENTS - filled);
        }

        @SubscribeEvent
        public static void onUseItem(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (!event.isUseItem() || player == null || mc.screen != null || player.isShiftKeyDown()) return;
            ItemStack tool = heldTool(player);
            if (tool.isEmpty() || !ToolTarget.hasSelection(tool)) return;
            InteractionHand hand = player.getMainHandItem().getItem() instanceof MultiblockToolItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.getHand() == hand && !player.getCooldowns().isOnCooldown(tool)) {
                ClientPacketDistributor.sendToServer(new ToolBuildPkt(hand));
            }
        }

        /** Mirrors the server's Shift+right-click confirmation so the machine is outlined while it waits. */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Level level = event.getLevel();
            Player player = event.getEntity();
            if (!level.isClientSide() || !player.isShiftKeyDown()
                    || !(player.getItemInHand(event.getHand()).getItem() instanceof MultiblockToolItem)) {
                return;
            }
            List<BlockPos> positions = DismantlePlanner.previewPositions(level, event.getPos(), player.getItemInHand(event.getHand()));
            if (positions.isEmpty()) {
                pendingController = null;
                return;
            }
            BlockPos controller = positions.get(positions.size() - 1);
            long now = level.getGameTime();
            if (controller.equals(pendingController) && now <= pendingUntil) {
                pendingController = null;
                pendingPositions = List.of();
                return;
            }
            pendingController = controller;
            pendingPositions = positions;
            pendingUntil = now + ToolDismantles.CONFIRM_TICKS;
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.level == null || mc.screen != null || !player.isShiftKeyDown() || heldTool(player).isEmpty()) {
                scrollAccum = 0;
                return;
            }
            if (event.getScrollDeltaY() == 0) {
                return;
            }
            event.setCanceled(true);
            double delta = event.getScrollDeltaY();
            long now = Util.getMillis();
            if (Math.signum(delta) != Math.signum(scrollAccum) || now - lastScrollMs > SCROLL_RESET_MS) {
                scrollAccum = 0;
            }
            lastScrollMs = now;
            scrollAccum += delta;
            while (Math.abs(scrollAccum) >= 1) {
                int step = scrollAccum > 0 ? 1 : -1;
                ClientPacketDistributor.sendToServer(new ToolRotatePkt(step));
                scrollAccum -= step;
            }
        }
    }
}
