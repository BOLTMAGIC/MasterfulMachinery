package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.PortConfigPkt;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Side panel drawn next to a port GUI: an unfolded cube of side toggles for auto I/O,
 * plus lock and dump buttons for lockable (fluid / chemical) storages.
 */
public class PortConfigPanel {
    private static final int BTN = 10;
    private static final int STEP = BTN + 1;
    private static final int PAD = 4;

    private static final int COLOR_BORDER = 0xFF373737;
    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_ON = 0xFF3F9B45;
    private static final int COLOR_OFF = 0xFF8B8B8B;
    private static final int COLOR_LOCKED = 0xFFC7962A;
    private static final int COLOR_DUMP = 0xFFB33A3A;
    private static final int COLOR_HOVER = 0x40FFFFFF;

    //        [U]
    //     [W][N][E]
    //        [D]
    //        [S]
    private static final Direction[] CUBE_SIDES = {Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN, Direction.SOUTH};
    private static final int[][] CUBE_CELLS = {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}, {1, 3}};

    private final AbstractPortBlockEntity be;
    private int x;
    private int y;

    public PortConfigPanel(AbstractPortBlockEntity be) {
        this.be = be;
    }

    public boolean isVisible() {
        return be.getAutoIO() != null || lockable() != null;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int width() {
        return PAD * 2 + STEP * 3 - 1;
    }

    public int height() {
        int h = PAD * 2 - 1;
        if (be.getAutoIO() != null) h += STEP * 4;
        if (lockable() != null) h += STEP + (be.getAutoIO() != null ? 3 : 0);
        return h;
    }

    public boolean isWithin(double mouseX, double mouseY) {
        return isVisible() && mouseX >= x && mouseX < x + width() && mouseY >= y && mouseY < y + height();
    }

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        if (!isVisible()) return;
        gfx.fill(x, y, x + width(), y + height(), COLOR_BORDER);
        gfx.fill(x + 1, y + 1, x + width() - 1, y + height() - 1, COLOR_BG);

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            for (int i = 0; i < CUBE_SIDES.length; i++) {
                Direction side = CUBE_SIDES[i];
                int bx = sideX(i), by = sideY(i);
                drawButton(gfx, font, bx, by, autoIO.isSideEnabled(side) ? COLOR_ON : COLOR_OFF,
                        Component.translatable("gui.mm.port.side." + side.getName() + ".short"), mouseX, mouseY);
            }
        }

        ILockablePortStorage lockable = lockable();
        if (lockable != null) {
            drawButton(gfx, font, lockX(), actionY(), lockable.isLocked() ? COLOR_LOCKED : COLOR_OFF,
                    Component.literal("L"), mouseX, mouseY);
            drawButton(gfx, font, dumpX(), actionY(), COLOR_DUMP, Component.literal("X"), mouseX, mouseY);
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

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            for (int i = 0; i < CUBE_SIDES.length; i++) {
                if (hit(mouseX, mouseY, sideX(i), sideY(i))) {
                    send(PortConfigPkt.Action.TOGGLE_SIDE, CUBE_SIDES[i].get3DDataValue());
                    return true;
                }
            }
        }

        if (lockable() != null) {
            if (hit(mouseX, mouseY, lockX(), actionY())) {
                send(PortConfigPkt.Action.TOGGLE_LOCK, 0);
            } else if (hit(mouseX, mouseY, dumpX(), actionY()) && Screen.hasShiftDown()) {
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

        PortAutoIO autoIO = be.getAutoIO();
        if (autoIO != null) {
            for (int i = 0; i < CUBE_SIDES.length; i++) {
                if (hit(mouseX, mouseY, sideX(i), sideY(i))) {
                    Direction side = CUBE_SIDES[i];
                    String state = !autoIO.isSideEnabled(side) ? "off" : autoIO.isPull() ? "pull" : "push";
                    var lines = new ArrayList<Component>();
                    lines.add(Component.translatable("gui.mm.port.side." + side.getName())
                            .append(": ")
                            .append(Component.translatable("gui.mm.port.side.state." + state)));
                    lines.add(Component.translatable("gui.mm.port.side.hint").withStyle(ChatFormatting.GRAY));
                    return lines;
                }
            }
        }

        ILockablePortStorage lockable = lockable();
        if (lockable != null) {
            if (hit(mouseX, mouseY, lockX(), actionY())) {
                return List.of(
                        Component.translatable(lockable.isLocked() ? "gui.mm.port.lock.on" : "gui.mm.port.lock.off"),
                        Component.translatable("gui.mm.port.lock.hint").withStyle(ChatFormatting.GRAY));
            }
            if (hit(mouseX, mouseY, dumpX(), actionY())) {
                return List.of(
                        Component.translatable("gui.mm.port.dump"),
                        Component.translatable("gui.mm.port.dump.hint").withStyle(ChatFormatting.RED));
            }
        }
        return null;
    }

    private void drawButton(GuiGraphics gfx, Font font, int bx, int by, int color, Component label, int mouseX, int mouseY) {
        gfx.fill(bx, by, bx + BTN, by + BTN, color);
        if (hit(mouseX, mouseY, bx, by)) {
            gfx.fill(bx, by, bx + BTN, by + BTN, COLOR_HOVER);
        }
        int textX = bx + (BTN - font.width(label)) / 2 + 1;
        gfx.drawString(font, label, textX, by + 1, 0xFFFFFFFF, false);
    }

    private void send(PortConfigPkt.Action action, int arg) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        MMNetwork.INSTANCE.sendToServer(new PortConfigPkt(be.getBlockPos(), action, arg));
    }

    @Nullable
    private ILockablePortStorage lockable() {
        return be.getStorage() instanceof ILockablePortStorage lockable ? lockable : null;
    }

    private static boolean hit(double mouseX, double mouseY, int bx, int by) {
        return mouseX >= bx && mouseX < bx + BTN && mouseY >= by && mouseY < by + BTN;
    }

    private int sideX(int i) {
        return x + PAD + CUBE_CELLS[i][0] * STEP;
    }

    private int sideY(int i) {
        return y + PAD + CUBE_CELLS[i][1] * STEP;
    }

    private int actionY() {
        return y + PAD + (be.getAutoIO() != null ? STEP * 4 + 3 : 0);
    }

    private int lockX() {
        return x + PAD;
    }

    private int dumpX() {
        return x + PAD + STEP * 2;
    }
}
