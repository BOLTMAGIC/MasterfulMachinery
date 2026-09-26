package io.ticticboom.mods.mm.client.tool;

import com.mojang.blaze3d.platform.InputConstants;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
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

    // V hold: the aimed-at block, its machine's positions, ticks held, whether the packet went out
    private static BlockPos holdTarget;
    private static List<BlockPos> holdPositions = List.of();
    private static int holdTicks;
    private static boolean holdSent;
    // first Shift+right-click on a machine: outlined until the server's confirmation window ends
    private static BlockPos pendingController;
    private static List<BlockPos> pendingPositions = List.of();
    private static long pendingUntil;
    private static long lastRotateTime = -ROTATE_PREVIEW_TICKS - 1;

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
        if (holdTarget != null) {
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

    private static List<BlockPos> machinePositions(Level level, BlockPos target) {
        MachineControllerBlockEntity controller = DismantlePlanner.resolve(level, target);
        return controller == null ? List.of() : DismantlePlanner.positions(level, controller);
    }

    private static void resetHold() {
        holdTarget = null;
        holdPositions = List.of();
        holdTicks = 0;
        holdSent = false;
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
                resetHold();
                pendingController = null;
                return;
            }
            boolean holding = !heldTool(player).isEmpty();
            if (!holding || mc.screen != null || !DISMANTLE.isDown()) {
                if (holdTarget != null && !holdSent) {
                    // released early: clear the progress bar
                    player.displayClientMessage(Component.empty(), true);
                }
                resetHold();
                return;
            }
            BlockPos target = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
            if (target == null) {
                resetHold();
                return;
            }
            if (!target.equals(holdTarget)) {
                // new aim: resolve once, not every frame (resolve scans nearby controllers)
                resetHold();
                holdTarget = target.immutable();
                holdPositions = machinePositions(level, holdTarget);
            }
            if (holdPositions.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.mm.tool.dismantle.no_machine"), true);
                return;
            }
            if (holdSent) {
                return;
            }
            holdTicks++;
            player.displayClientMessage(Component.translatable("message.mm.tool.dismantle.progress", bar(holdTicks)), true);
            if (holdTicks >= HOLD_TICKS) {
                MMNetwork.INSTANCE.sendToServer(new ToolDismantlePkt(holdTarget));
                holdSent = true;
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
            if (!level.isClientSide() || !player.isShiftKeyDown() || !(event.getItemStack().getItem() instanceof MultiblockToolItem)) {
                return;
            }
            List<BlockPos> positions = machinePositions(level, event.getPos());
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
            MMNetwork.INSTANCE.sendToServer(new ToolRotatePkt(event.getScrollDelta() > 0 ? 1 : -1));
            lastRotateTime = mc.level.getGameTime();
        }
    }
}
