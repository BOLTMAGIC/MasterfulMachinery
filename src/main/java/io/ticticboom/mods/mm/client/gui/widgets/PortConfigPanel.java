package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Port settings: a button in the title row opens a panel glued to the right edge of the port GUI
 * (never covering the port's contents) with an unfolded cube of auto I/O side toggles, plus lock
 * and dump for lockable (fluid / chemical) storages. Drawn with Masterful Machinery's own textures.
 */
public class PortConfigPanel {
    private static final int BTN = 12;
    private static final int STEP = BTN + 1;
    private static final int TOGGLE_X = 155;
    private static final int TOGGLE_Y = 5;
    private static final int PANEL_X = 170;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 58;
    private static final int CUBE_X = 10;
    private static final int CUBE_Y = 18;
    private static final int TEXT = 0x404040;
    private static final int WINDOW_BG = 0xFFC6C6C6;

    // Unfolded cube, relative to the multiblock's controller when the port is part of one:
    //          [Top]
    //   [Left][Front][Right]
    //         [Bottom]
    //          [Back]
    // otherwise by compass (Up / West North East / Down / South).
    private static final PortSides.Relative[] CUBE_RELATIVE = {
            PortSides.Relative.TOP, PortSides.Relative.LEFT, PortSides.Relative.FRONT,
            PortSides.Relative.RIGHT, PortSides.Relative.BOTTOM, PortSides.Relative.BACK};
    private static final Direction[] CUBE_COMPASS = {Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN, Direction.SOUTH};
    private static final int[][] CUBE_CELLS = {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}, {1, 3}};

    // remembered while the game runs, so the panel stays open across port GUIs
    private static boolean open = false;

    private final AbstractPortBlockEntity be;
    private int guiLeft;
    private int guiTop;

    public PortConfigPanel(AbstractPortBlockEntity be) {
        this.be = be;
    }

    /**
     * Draws the port's name on one line, cut short so it never runs under the settings button.
     * Coordinates are relative to the GUI (for use in renderLabels).
     */
    public static void drawTitle(GuiGraphics gfx, Font font, String name) {
        FormattedText title = font.ellipsize(FormattedText.of(name), TOGGLE_X - 11);
        gfx.drawString(font, Language.getInstance().getVisualOrder(title), 8, 8, TEXT, false);
    }

    public boolean isVisible() {
        return be.getAutoIO() != null || lockable() != null;
    }

    /**
     * @param guiLeft left edge of the port GUI
     * @param guiTop  top edge of the port GUI
     */
    public void setPosition(int guiLeft, int guiTop) {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
    }

    public boolean isWithin(double mouseX, double mouseY) {
        if (!isVisible()) return false;
        if (hit(mouseX, mouseY, toggleX(), toggleY())) return true;
        return open && mouseX >= panelX() && mouseX < panelX() + PANEL_W && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        if (!isVisible()) return;
        drawButton(gfx, font, toggleX(), toggleY(), open, Component.literal("="));
        if (!open) return;

        int h = panelHeight();
        gfx.blitNineSlicedSized(Ref.UiTextures.TILING_GUI, panelX(), panelY(), PANEL_W, h, 4, 4, 4, 4, 12, 12, 0, 0, 12, 12);
        // hide the seam so the panel reads as part of the window
        gfx.fill(guiLeft + PANEL_X - 2, panelY() + 4, guiLeft + PANEL_X + 4, panelY() + h - 4, WINDOW_BG);
        gfx.drawString(font, Component.translatable("gui.mm.port.panel.title"), panelX() + 6, panelY() + 6, TEXT, false);

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            Direction[] sides = cubeSides(autoIO);
            for (int i = 0; i < sides.length; i++) {
                drawButton(gfx, font, sideX(i), sideY(i), autoIO.isSideEnabled(sides[i]),
                        Component.translatable(PortSides.shortKey(sides[i], autoIO.getFront())));
            }
        }

        ILockablePortStorage lockable = lockable();
        if (lockable != null) {
            drawButton(gfx, font, actionX(), lockY(), lockable.isLocked(), Component.literal("L"));
            gfx.drawString(font, Component.translatable("gui.mm.port.lock"), actionX() + BTN + 3, lockY() + 2, TEXT, false);
            drawButton(gfx, font, actionX(), dumpY(), false, Component.literal("X"));
            gfx.drawString(font, Component.translatable("gui.mm.port.dump.short"), actionX() + BTN + 3, dumpY() + 2, TEXT, false);
        }
    }

    public void renderTooltip(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        List<Component> tooltip = tooltipAt(mouseX, mouseY);
        if (tooltip != null) {
            gfx.renderComponentTooltip(font, tooltip, mouseX, mouseY);
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
            Direction[] sides = cubeSides(autoIO);
            for (int i = 0; i < sides.length; i++) {
                if (hit(mouseX, mouseY, sideX(i), sideY(i))) {
                    send(PortConfigPkt.Action.TOGGLE_SIDE, sides[i].get3DDataValue());
                    return true;
                }
            }
        }

        if (lockable() != null) {
            if (hit(mouseX, mouseY, actionX(), lockY())) {
                send(PortConfigPkt.Action.TOGGLE_LOCK, 0);
            } else if (hit(mouseX, mouseY, actionX(), dumpY()) && Screen.hasShiftDown()) {
                // dumping deletes contents permanently, so it needs shift as a guard against misclicks
                send(PortConfigPkt.Action.DUMP, 0);
            }
        }
        // swallow clicks on the panel background so they don't count as clicking outside the GUI
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
            Direction[] sides = cubeSides(autoIO);
            for (int i = 0; i < sides.length; i++) {
                if (hit(mouseX, mouseY, sideX(i), sideY(i))) {
                    Direction side = sides[i];
                    String state = !autoIO.isSideEnabled(side) ? "off" : autoIO.isPull() ? "pull" : "push";
                    var lines = new ArrayList<Component>();
                    lines.add(Component.translatable(PortSides.nameKey(side, autoIO.getFront()))
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

    private static void drawButton(GuiGraphics gfx, Font font, int bx, int by, boolean pressed, Component label) {
        (pressed ? GuiTextures.BUTTON_PRESSED : GuiTextures.BUTTON_ACTIVE).blit(gfx, bx, by, BTN, BTN);
        int textX = bx + (BTN - font.width(label)) / 2 + 1;
        gfx.drawString(font, label, textX, by + 2, pressed ? 0xFFFFFF : TEXT, false);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void send(PortConfigPkt.Action action, int arg) {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new PortConfigPkt(be.getBlockPos(), action, arg));
    }

    @Nullable
    private ILockablePortStorage lockable() {
        return be.getStorage() instanceof ILockablePortStorage lockable ? lockable : null;
    }

    /**
     * @return the world direction of each cube cell, in {@link #CUBE_CELLS} order
     */
    private static Direction[] cubeSides(PortAutoIO autoIO) {
        Direction front = autoIO.getFront();
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
        if (be.getAutoIO() != null) h += STEP * 4 + 3;
        if (lockable() != null) h += STEP * 2 + 1;
        return h + 3;
    }

    private int toggleX() {
        return guiLeft + TOGGLE_X;
    }

    private int toggleY() {
        return guiTop + TOGGLE_Y;
    }

    private int panelX() {
        return guiLeft + PANEL_X;
    }

    private int panelY() {
        return guiTop + PANEL_Y;
    }

    private int sideX(int i) {
        return panelX() + CUBE_X + CUBE_CELLS[i][0] * STEP;
    }

    private int sideY(int i) {
        return panelY() + CUBE_Y + CUBE_CELLS[i][1] * STEP;
    }

    private int actionX() {
        return panelX() + 6;
    }

    private int lockY() {
        return panelY() + CUBE_Y + (be.getAutoIO() != null ? STEP * 4 + 3 : 0);
    }

    private int dumpY() {
        return lockY() + STEP + 1;
    }
}
