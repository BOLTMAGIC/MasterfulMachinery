package io.ticticboom.mods.mm.client.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import io.ticticboom.mods.mm.client.util.NineSliceUtil;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PortConfigPanel {
    private static final int BTN = 12;
    private static final int STEP = BTN + 1;
    private static final int TOGGLE_RIGHT = 19;
    private static final int TOGGLE_Y = 5;
    private static final int PANEL_RIGHT = 4;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 58;
    private static final int CUBE_Y = 18;
    private static final int SIDE = 14;
    private static final int CENTER_X = 20;
    private static final int CENTER_Y = CUBE_Y + 15;
    private static final int CUBE_H = 64 + 12;
    private static final int PULL_COLOR = 0xFF2F6FD6;
    private static final int PUSH_COLOR = 0xFFD9822B;
    private static final int TEXT = 0x404040;
    private static final int WINDOW_BG = 0xFFC6C6C6;

    private static final PortSides.Relative[] CUBE_RELATIVE = {
            PortSides.Relative.TOP, PortSides.Relative.LEFT, PortSides.Relative.FRONT,
            PortSides.Relative.RIGHT, PortSides.Relative.BOTTOM, PortSides.Relative.BACK};
    private static final Direction[] CUBE_COMPASS = {Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN, Direction.SOUTH};
    private static final int[][] SIDE_POS = {{22, CUBE_Y}, {5, CUBE_Y + 17}, {5, CUBE_Y + 49}, {39, CUBE_Y + 17}, {22, CUBE_Y + 34}, {39, CUBE_Y + 49}};

    private static boolean open = false;

    private final AbstractPortBlockEntity be;
    private int guiLeft;
    private int guiTop;
    private int guiWidth;

    public PortConfigPanel(AbstractPortBlockEntity be) {
        this.be = be;
    }

    public static void drawTitle(GuiGraphicsExtractor gfx, Font font, Component name, int guiWidth) {
        FormattedText title = font.ellipsize(name, guiWidth - TOGGLE_RIGHT - 11);
        gfx.text(font, Language.getInstance().getVisualOrder(title), 8, 8, TextColors.argb(TEXT), false);
    }

    public boolean isVisible() {
        return be.getAutoIO() != null || lockable() != null;
    }

    public void setPosition(int guiLeft, int guiTop, int guiWidth) {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.guiWidth = guiWidth;
    }

    public boolean isWithin(double mouseX, double mouseY) {
        if (!isVisible()) return false;
        if (hit(mouseX, mouseY, toggleX(), toggleY())) return true;
        return open && mouseX >= panelX() && mouseX < panelX() + PANEL_W && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY) {
        if (!isVisible()) return;
        gfx.pose().pushMatrix();
        gfx.pose().translate(0, 0);
        drawButton(gfx, font, toggleX(), toggleY(), open, Component.literal("="));
        if (!open) {
            gfx.pose().popMatrix();
            return;
        }

        int h = panelHeight();
        NineSliceUtil.blitNineSliced(gfx, Ref.UiTextures.TILING_GUI, panelX(), panelY(), PANEL_W, h, 4, 4, 4, 4, 12, 12, 0, 0, 12, 12);
        gfx.fill(panelX() - 2, panelY() + 4, panelX() + 4, panelY() + h - 4, WINDOW_BG);
        gfx.text(font, Component.translatable("gui.mm.port.panel.title"), panelX() + 6, panelY() + 6, TextColors.argb(TEXT), false);

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            int cx = panelX() + CENTER_X;
            int cy = panelY() + CENTER_Y;
            gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, cx, cy, 0, 26, 18, 18, 256, 256);
            gfx.item(new ItemStack(be.getBlockState().getBlock()), cx + 1, cy + 1);

            int color = autoIO.isPull() ? PULL_COLOR : PUSH_COLOR;
            Direction[] sides = cubeSides();
            for (int i = 0; i < sides.length; i++) {
                boolean on = autoIO.isSideEnabled(sides[i]);
                boolean hovered = hitSide(mouseX, mouseY, sideX(i), sideY(i));
                drawSide(gfx, font, sideX(i), sideY(i), on, hovered, color,
                        Component.translatable(PortSides.shortKey(sides[i], be.getMachineFront())));
            }

            int ly = panelY() + CUBE_Y + 66;
            gfx.fill(panelX() + 6, ly + 1, panelX() + 12, ly + 7, color);
            gfx.text(font, Component.translatable(autoIO.isPull() ? "gui.mm.port.side.legend.pull" : "gui.mm.port.side.legend.push"), panelX() + 15, ly, TextColors.argb(TEXT), false);
        }

        ILockablePortStorage lockable = lockable();
        if (lockable != null) {
            drawButton(gfx, font, actionX(), lockY(), lockable.isLocked(), Component.literal("L"));
            gfx.text(font, Component.translatable("gui.mm.port.lock"), actionX() + BTN + 3, lockY() + 2, TextColors.argb(TEXT), false);
            drawButton(gfx, font, actionX(), dumpY(), false, Component.literal("X"));
            gfx.text(font, Component.translatable("gui.mm.port.dump.short"), actionX() + BTN + 3, dumpY() + 2, TextColors.argb(TEXT), false);
        }
        gfx.pose().popMatrix();
    }

    private static void drawSide(GuiGraphicsExtractor gfx, Font font, int bx, int by, boolean on, boolean hovered, int color, Component label) {
        (hovered ? GuiTextures.BUTTON_PRESSED : GuiTextures.BUTTON_ACTIVE).blit(gfx, bx, by, SIDE, SIDE);
        if (on) {
            gfx.fill(bx + 1, by + 1, bx + SIDE - 1, by + SIDE - 1, color);
        }
        int textX = bx + (SIDE - font.width(label)) / 2 + 1;
        gfx.text(font, label, textX, by + 3, TextColors.argb(on ? 0xFFFFFF : TEXT), on);
    }

    private static boolean hitSide(double mouseX, double mouseY, int bx, int by) {
        return mouseX >= bx && mouseX < bx + SIDE && mouseY >= by && mouseY < by + SIDE;
    }

    public void extractTooltip(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY) {
        List<Component> tooltip = tooltipAt(mouseX, mouseY);
        if (tooltip != null) {
            gfx.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isWithin(mouseX, mouseY)) return false;

        if (hit(mouseX, mouseY, toggleX(), toggleY())) {
            open = !open;
            playClick();
            return true;
        }

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            Direction[] sides = cubeSides();
            for (int i = 0; i < sides.length; i++) {
                if (hitSide(mouseX, mouseY, sideX(i), sideY(i))) {
                    send(PortConfigPkt.Action.TOGGLE_SIDE, sides[i].get3DDataValue());
                    return true;
                }
            }
        }

        if (lockable() != null) {
            if (hit(mouseX, mouseY, actionX(), lockY())) {
                send(PortConfigPkt.Action.TOGGLE_LOCK, 0);
            } else if (hit(mouseX, mouseY, actionX(), dumpY()) && Minecraft.getInstance().hasShiftDown()) {
                send(PortConfigPkt.Action.DUMP, 0);
            }
        }
        return true;
    }

    @Nullable
    private List<Component> tooltipAt(int mouseX, int mouseY) {
        if (!isWithin(mouseX, mouseY)) return null;

        if (hit(mouseX, mouseY, toggleX(), toggleY())) {
            return List.of(Component.translatable("gui.mm.port.panel.toggle"));
        }

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            Direction[] sides = cubeSides();
            for (int i = 0; i < sides.length; i++) {
                if (hitSide(mouseX, mouseY, sideX(i), sideY(i))) {
                    Direction side = sides[i];
                    String state = !autoIO.isSideEnabled(side) ? "off" : autoIO.isPull() ? "pull" : "push";
                    var lines = new ArrayList<Component>();
                    lines.add(Component.translatable(PortSides.nameKey(side, be.getMachineFront()))
                            .append(": ")
                            .append(Component.translatable("gui.mm.port.side.state." + state)));
                    lines.add(Component.translatable("gui.mm.port.side.hint").withStyle(ChatFormatting.GRAY));
                    return lines;
                }
            }
        }

        ILockablePortStorage lockable = lockable();
        if (lockable != null) {
            if (hit(mouseX, mouseY, actionX(), lockY())) {
                return List.of(
                        Component.translatable(lockable.isLocked() ? "gui.mm.port.lock.on" : "gui.mm.port.lock.off"),
                        Component.translatable("gui.mm.port.lock.hint").withStyle(ChatFormatting.GRAY));
            }
            if (hit(mouseX, mouseY, actionX(), dumpY())) {
                return List.of(
                        Component.translatable("gui.mm.port.dump"),
                        Component.translatable("gui.mm.port.dump.hint").withStyle(ChatFormatting.RED));
            }
        }
        return null;
    }

    private static void drawButton(GuiGraphicsExtractor gfx, Font font, int bx, int by, boolean pressed, Component label) {
        (pressed ? GuiTextures.BUTTON_PRESSED : GuiTextures.BUTTON_ACTIVE).blit(gfx, bx, by, BTN, BTN);
        int textX = bx + (BTN - font.width(label)) / 2 + 1;
        gfx.text(font, label, textX, by + 2, TextColors.argb(pressed ? 0xFFFFFF : TEXT), false);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void send(PortConfigPkt.Action action, int arg) {
        playClick();
        ClientPacketDistributor.sendToServer(new PortConfigPkt(be.getBlockPos(), action, arg));
    }

    @Nullable
    private ILockablePortStorage lockable() {
        return be.getStorage() instanceof ILockablePortStorage lockable ? lockable : null;
    }

    private Direction[] cubeSides() {
        Direction front = be.getMachineFront();
        if (front == null) {
            return CUBE_COMPASS;
        }
        Direction[] sides = new Direction[CUBE_RELATIVE.length];
        for (int i = 0; i < sides.length; i++) {
            sides[i] = PortSides.toWorld(CUBE_RELATIVE[i], front);
        }
        return sides;
    }

    private static boolean hit(double mouseX, double mouseY, int bx, int by) {
        return mouseX >= bx && mouseX < bx + BTN && mouseY >= by && mouseY < by + BTN;
    }

    private int panelHeight() {
        int h = CUBE_Y;
        if (be.getAutoIO() != null) h += CUBE_H;
        if (lockable() != null) h += STEP * 2 + 1;
        return h + 3;
    }

    private int toggleX() {
        return guiLeft + guiWidth - TOGGLE_RIGHT;
    }

    private int toggleY() {
        return guiTop + TOGGLE_Y;
    }

    private int panelX() {
        return guiLeft + guiWidth - PANEL_RIGHT;
    }

    private int panelY() {
        return guiTop + PANEL_Y;
    }

    private int sideX(int i) {
        return panelX() + SIDE_POS[i][0];
    }

    private int sideY(int i) {
        return panelY() + SIDE_POS[i][1];
    }

    private int actionX() {
        return panelX() + 6;
    }

    private int lockY() {
        return panelY() + CUBE_Y + (be.getAutoIO() != null ? CUBE_H : 0);
    }

    private int dumpY() {
        return lockY() + STEP + 1;
    }
}
