package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.me.HudState;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Multiblock Tool's craft progress above the hotbar, fed by {@code ToolHudPkt}: "Crafting ▓▓▓░░ 5/8 items" with the
 * items still in progress below it, "Waiting for the ME network…" (with the items that wait) while the network can't be
 * reached, a yellow failure line for {@value #SHOW_MS} ms when a craft fails, and the green "Ready — right-click to
 * build" for {@value #SHOW_MS} ms once all are done (the only place that says so). It sits above the action bar and the
 * health/food rows, however high those are stacked.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ToolHudOverlay {
    public static final String ID = "tool_hud";
    public static final IGuiOverlay OVERLAY = ToolHudOverlay::render;
    private static final long SHOW_MS = 5000;
    private static final int BAR_SEGMENTS = 10;
    /** The action bar's text ends this far above the bottom. */
    private static final int ACTION_BAR = 72;
    private static final int GAP = 4;
    private static final int PAD = 3;
    private static final int ICON = 16;
    private static final int PANEL = 0x80000000;
    private static final int TEXT = 0xE0E0E0;
    private static final int WAITING = 0xFFD84D;

    private static HudState state = HudState.NONE;
    private static long phaseSince;
    private static long failureSince;

    private ToolHudOverlay() {
    }

    /** A new state from the server; "Ready" and failure lines are timed from when they first arrive. */
    public static void receive(HudState next) {
        long now = Util.getMillis();
        if (next.phase() != state.phase()) {
            phaseSince = now;
        }
        if (next.failure() != null && !next.failure().equals(state.failure())) {
            failureSince = now;
        }
        state = next;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = HudState.NONE;
    }

    /** The lines shown now, top to bottom (the icons row is separate); empty when the HUD is hidden. */
    private static List<Component> lines() {
        long now = Util.getMillis();
        boolean failureShown = state.failure() != null && now - failureSince < SHOW_MS;
        List<Component> lines = new ArrayList<>();
        switch (state.phase()) {
            case CRAFTING, WAITING -> {
                if (failureShown) {
                    lines.add(state.failure().copy().withStyle(ChatFormatting.YELLOW));
                }
                lines.add(state.phase() == HudState.Phase.WAITING
                        ? Component.translatable("message.mm.tool.hud.waiting").withStyle(s -> s.withColor(WAITING))
                        : Component.translatable("message.mm.tool.hud.crafting", bar(state.done(), state.total()), state.done(), state.total()));
            }
            case READY -> {
                if (now - phaseSince < SHOW_MS) {
                    lines.add(Component.translatable("message.mm.tool.hud.ready").withStyle(ChatFormatting.GREEN));
                }
            }
            case FAILED -> {
                if (failureShown) {
                    lines.add(state.failure().copy().withStyle(ChatFormatting.YELLOW));
                }
            }
            case NONE -> {
            }
        }
        return lines;
    }

    private static List<Item> icons() {
        // while waiting too: what is stuck
        boolean onItsWay = state.phase() == HudState.Phase.CRAFTING || state.phase() == HudState.Phase.WAITING;
        return onItsWay ? state.inProgress() : List.of();
    }

    private static String bar(int done, int total) {
        int filled = total <= 0 ? 0 : Math.min(BAR_SEGMENTS, done * BAR_SEGMENTS / total);
        return "▓".repeat(filled) + "░".repeat(BAR_SEGMENTS - filled);
    }

    private static void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        List<Component> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        Font font = mc.font;
        List<Item> icons = icons();
        int lineHeight = font.lineHeight + 2;
        int contentHeight = lines.size() * lineHeight + (icons.isEmpty() ? 0 : ICON + 2);
        int contentWidth = icons.size() * (ICON + 2);
        for (Component line : lines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }
        // above the action bar and whatever the health, armour, food and air rows take
        int bottom = height - Math.max(ACTION_BAR, Math.max(gui.leftHeight, gui.rightHeight)) - GAP;
        int top = bottom - contentHeight;
        int center = width / 2;
        gfx.fill(center - contentWidth / 2 - PAD, top - PAD, center + (contentWidth + 1) / 2 + PAD, bottom + PAD - 2, PANEL);
        int y = top;
        for (Component line : lines) {
            gfx.drawString(font, line, center - font.width(line) / 2, y + 1, TEXT, true);
            y += lineHeight;
        }
        int x = center - (icons.size() * (ICON + 2) - 2) / 2;
        for (Item item : icons) {
            gfx.renderItem(new ItemStack(item), x, y);
            x += ICON + 2;
        }
    }
}
