package io.ticticboom.mods.mm.client.tool;

import com.mojang.blaze3d.platform.InputConstants;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToolDismantlePkt;
import io.ticticboom.mods.mm.net.packet.ToolRotatePkt;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolDismantles;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

/**
 * The multiblock tool's key: hold it while looking at a machine to dismantle it. Also tracks which machine a
 * dismantle is aimed at (V held, or waiting for the second Shift+right-click) so the hologram can outline it.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ToolKeys {
    public static final KeyMapping DISMANTLE = new KeyMapping("key.mm.dismantle", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.mm");
    /** How long V must be held: 1 s. */
    public static final int HOLD_TICKS = 20;
    private static final int BAR_SEGMENTS = 10;
    /** How long the hologram stays visible while sneaking after a Shift+scroll: 2 s. */
    private static final int ROTATE_PREVIEW_TICKS = 40;

    // V hold: the aimed-at block, the machine's controller and positions, ticks held on that machine
    private static BlockPos holdAim;
    private static BlockPos holdController;
    private static List<BlockPos> holdPositions = List.of();
    private static int holdTicks;
    // set once the packet went out; nothing more happens until V is released
    private static boolean holdLatched;
    private static boolean barShown;
    private static boolean noMachineShown;
    // first Shift+right-click on a machine: outlined until the server's confirmation window ends
    private static BlockPos pendingController;
    private static List<BlockPos> pendingPositions = List.of();
    private static long pendingUntil;
    private static long lastRotateTime = -ROTATE_PREVIEW_TICKS - 1;
    private static double scrollAccum;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(DISMANTLE);
    }

    /** The tool in the main hand, else the off hand; empty when the player holds none. */
    public static ItemStack heldTool(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MultiblockToolItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Positions of the machine a dismantle is aimed at, to outline in red; empty when none. */
    static List<BlockPos> dismantleHighlight(Level level) {
        if (holdController != null) {
            return holdPositions;
        }
        if (pendingController != null && level.getGameTime() <= pendingUntil) {
            return pendingPositions;
        }
        return List.of();
    }

    /** True shortly after a Shift+scroll, so the hologram can show the new rotation while sneaking. */
    static boolean recentlyRotated(Level level) {
        long since = level.getGameTime() - lastRotateTime;
        return since >= 0 && since <= ROTATE_PREVIEW_TICKS;
    }

    /** Forgets the aimed-at machine and clears the progress bar if it was showing. */
    private static void loseTarget(Player player) {
        if (barShown) {
            player.displayClientMessage(Component.empty(), true);
            barShown = false;
        }
        holdAim = null;
        holdController = null;
        holdPositions = List.of();
        holdTicks = 0;
    }

    /** Client-side input handling; the server re-validates everything. */
    @Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Input {
        private Input() {
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
                holdAim = null;
                holdController = null;
                holdPositions = List.of();
                holdTicks = 0;
                holdLatched = false;
                barShown = false;
                noMachineShown = false;
                pendingController = null;
                return;
            }
            if (heldTool(player).isEmpty() || mc.screen != null || !DISMANTLE.isDown()) {
                // released (early or not): the next press starts over
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
                // new aim: resolve once, not every tick; progress carries over while it is the same machine
                holdAim = target.immutable();
                List<BlockPos> positions = DismantlePlanner.previewPositions(level, holdAim);
                BlockPos controller = positions.isEmpty() ? null : positions.get(positions.size() - 1);
                if (!Objects.equals(controller, holdController)) {
                    holdTicks = 0;
                }
                holdController = controller;
                holdPositions = positions;
            }
            if (holdController == null) {
                if (barShown) {
                    player.displayClientMessage(Component.empty(), true);
                    barShown = false;
                }
                if (!noMachineShown) {
                    player.displayClientMessage(Component.translatable("message.mm.tool.dismantle.no_machine"), true);
                    noMachineShown = true;
                }
                return;
            }
            holdTicks++;
            player.displayClientMessage(Component.translatable("message.mm.tool.dismantle.progress", bar(holdTicks)), true);
            barShown = true;
            if (holdTicks >= HOLD_TICKS) {
                // the server resolves the machine again from the aimed-at block
                MMNetwork.INSTANCE.sendToServer(new ToolDismantlePkt(holdAim));
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

        /** Mirrors the server's Shift+right-click confirmation so the machine is outlined while it waits. */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Level level = event.getLevel();
            Player player = event.getEntity();
            if (!level.isClientSide() || !player.isShiftKeyDown()
                    || !(player.getItemInHand(event.getHand()).getItem() instanceof MultiblockToolItem)) {
                return;
            }
            List<BlockPos> positions = DismantlePlanner.previewPositions(level, event.getPos());
            if (positions.isEmpty()) {
                pendingController = null;
                return;
            }
            // the controller is always last
            BlockPos controller = positions.get(positions.size() - 1);
            long now = level.getGameTime();
            if (controller.equals(pendingController) && now <= pendingUntil) {
                // second click: the server starts dismantling
                pendingController = null;
                pendingPositions = List.of();
                return;
            }
            pendingController = controller;
            pendingPositions = positions;
            pendingUntil = now + ToolDismantles.CONFIRM_TICKS;
        }

        /** Shift+scroll turns the build instead of changing the hotbar slot. */
        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.level == null || mc.screen != null || !player.isShiftKeyDown()
                    || event.getScrollDelta() == 0 || heldTool(player).isEmpty()) {
                return;
            }
            event.setCanceled(true);
            double delta = event.getScrollDelta();
            if (Math.signum(delta) != Math.signum(scrollAccum)) {
                scrollAccum = 0;
            }
            // one quarter turn per whole notch, so smooth-scrolling touchpads do not flood the server
            scrollAccum += delta;
            while (Math.abs(scrollAccum) >= 1) {
                int step = scrollAccum > 0 ? 1 : -1;
                MMNetwork.INSTANCE.sendToServer(new ToolRotatePkt(step));
                scrollAccum -= step;
            }
            lastRotateTime = mc.level.getGameTime();
        }
    }
}
