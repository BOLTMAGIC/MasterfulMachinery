package io.ticticboom.mods.mm.client.config;

import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.config.MMConfigOptions;
import io.ticticboom.mods.mm.config.WorkingEffectsConfig;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.MMConfigEditPkt;
import io.ticticboom.mods.mm.net.packet.MMConfigRequestPkt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-game MM settings, with a collapsed machine-effects list to keep the page compact. */
public final class MMConfigScreen extends Screen {
    private enum Page { SERVER, CLIENT, EFFECTS }
    private record EffectRow(ResourceLocation id, String name, @Nullable String field) {}
    private static final int ROW = 17;
    private static final int BG = 0xFF202126;
    private static final int PANEL = 0xFF303139;
    private static final int TEXT = 0xFFE4E4E4;
    private static final int MUTED = 0xFFA8A8A8;
    private static final int ACTIVE = 0xFF526785;
    private static final int ERROR = 0xFFFF7777;

    private final Screen parent;
    private Page page = Page.CLIENT;
    @Nullable private Map<String, String> serverValues;
    @Nullable private MMConfigOptions.Option editing;
    @Nullable private EffectRow editingEffect;
    @Nullable private ResourceLocation expandedController;
    @Nullable private EditBox editor;
    @Nullable private Component error;
    private int scroll;
    private int x;
    private int y;
    private int panelWidth;
    private int panelHeight;

    public MMConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("config.mm.title"));
        this.parent = parent;
    }

    public static void receive(Map<String, String> values) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof MMConfigScreen screen) screen.serverValues = Map.copyOf(values);
    }

    private boolean canEditServer() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(470, width - 24);
        panelHeight = Math.min(360, height - 24);
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        page = canEditServer() ? Page.SERVER : Page.CLIENT;
        if (canEditServer()) MMNetwork.INSTANCE.sendToServer(new MMConfigRequestPkt());
    }

    private List<MMConfigOptions.Option> options() {
        return page == Page.SERVER ? MMConfigOptions.SERVER : MMConfigOptions.CLIENT;
    }

    private List<EffectRow> effectRows() {
        Map<ResourceLocation, String> controllers = new LinkedHashMap<>();
        for (var holder : MMControllerRegistry.CONTROLLERS) {
            var block = holder.getBlock().get();
            if (block instanceof IControllerPart) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                if (id != null) controllers.put(id, block.getName().getString());
            }
        }
        for (ResourceLocation id : WorkingEffectsConfig.snapshot().keySet())
            controllers.putIfAbsent(id, id.toString());
        var sorted = new ArrayList<>(controllers.entrySet());
        sorted.sort(Comparator.comparing(entry -> entry.getValue().toLowerCase(java.util.Locale.ROOT)));
        List<EffectRow> rows = new ArrayList<>();
        for (var entry : sorted) {
            rows.add(new EffectRow(entry.getKey(), entry.getValue(), null));
            if (entry.getKey().equals(expandedController)) {
                rows.add(new EffectRow(entry.getKey(), entry.getValue(), "sound"));
                rows.add(new EffectRow(entry.getKey(), entry.getValue(), "interval"));
                rows.add(new EffectRow(entry.getKey(), entry.getValue(), "particle"));
            }
        }
        return rows;
    }

    private int rowsTop() { return y + 48; }
    private int rowsBottom() { return y + panelHeight - 37; }
    private int maxScroll() {
        int count = page == Page.EFFECTS ? effectRows().size() : options().size();
        return Math.max(0, count * ROW - (rowsBottom() - rowsTop()));
    }
    private int valueX() { return x + panelWidth - 136; }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        gfx.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFFC6C6C6);
        gfx.fill(x, y, x + panelWidth, y + panelHeight, BG);
        gfx.drawString(font, title, x + 9, y + 8, TEXT, false);
        button(gfx, x + 8, y + 25, 90, 16, Component.translatable("config.mm.server"),
                page == Page.SERVER, canEditServer(), mouseX, mouseY);
        button(gfx, x + 102, y + 25, 90, 16, Component.translatable("config.mm.client"),
                page == Page.CLIENT, true, mouseX, mouseY);
        button(gfx, x + 196, y + 25, 90, 16, Component.translatable("config.mm.effects"),
                page == Page.EFFECTS, true, mouseX, mouseY);
        button(gfx, x + panelWidth - 62, y + 7, 54, 16, Component.translatable("gui.done"),
                false, true, mouseX, mouseY);

        gfx.fill(x + 6, rowsTop() - 2, x + panelWidth - 6, rowsBottom() + 2, PANEL);
        if (page == Page.SERVER && serverValues == null) {
            gfx.drawString(font, Component.translatable("config.mm.loading"), x + 12, rowsTop() + 8, MUTED, false);
        } else if (page == Page.EFFECTS) renderEffects(gfx, mouseX, mouseY);
        else renderOptions(gfx, mouseX, mouseY);
        Component footer = error != null ? error : footerText(mouseX, mouseY);
        TextRenderUtil.drawClipped(gfx, font, footer, x + 9, y + panelHeight - 26, panelWidth - 18,
                error != null ? ERROR : MUTED);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderOptions(GuiGraphics gfx, int mouseX, int mouseY) {
        List<MMConfigOptions.Option> options = options();
        gfx.enableScissor(x + 7, rowsTop(), x + panelWidth - 7, rowsBottom());
        for (int i = 0; i < options.size(); i++) {
            int rowY = rowsTop() + i * ROW - scroll;
            if (rowY + ROW <= rowsTop() || rowY >= rowsBottom()) continue;
            MMConfigOptions.Option option = options.get(i);
            if (mouseX >= x + 8 && mouseX < x + panelWidth - 8 && mouseY >= rowY && mouseY < rowY + ROW)
                gfx.fill(x + 8, rowY, x + panelWidth - 8, rowY + ROW, 0xFF3B404A);
            Component label = Component.translatableWithFallback("config.mm.option." + option.key(), option.label());
            TextRenderUtil.drawClipped(gfx, font, label, x + 12, rowY + 4, valueX() - x - 18, TEXT);
            if (editing != option) {
                String value = page == Page.SERVER ? serverValues.getOrDefault(option.key(), "?") : option.current();
                gfx.fill(valueX(), rowY + 1, x + panelWidth - 12, rowY + ROW - 1, 0xFF484B54);
                Component valueLabel = option.kind() == MMConfigOptions.Kind.BUILD_MODE
                        ? Component.translatable("config.mm.build_mode." + value) : Component.literal(value);
                TextRenderUtil.drawClipped(gfx, font, valueLabel, valueX() + 4, rowY + 4, 112,
                        option.kind() == MMConfigOptions.Kind.BOOLEAN && value.equals("true") ? 0xFF79D889 : TEXT);
            }
        }
        gfx.disableScissor();
        drawScrollbar(gfx, options.size());
    }

    private void renderEffects(GuiGraphics gfx, int mouseX, int mouseY) {
        List<EffectRow> rows = effectRows();
        gfx.enableScissor(x + 7, rowsTop(), x + panelWidth - 7, rowsBottom());
        for (int i = 0; i < rows.size(); i++) {
            int rowY = rowsTop() + i * ROW - scroll;
            if (rowY + ROW <= rowsTop() || rowY >= rowsBottom()) continue;
            EffectRow row = rows.get(i);
            if (mouseX >= x + 8 && mouseX < x + panelWidth - 8 && mouseY >= rowY && mouseY < rowY + ROW)
                gfx.fill(x + 8, rowY, x + panelWidth - 8, rowY + ROW, 0xFF3B404A);
            if (row.field() == null) {
                String arrow = row.id().equals(expandedController) ? "▾ " : "▸ ";
                TextRenderUtil.drawClipped(gfx, font, Component.literal(arrow + row.name()), x + 12, rowY + 4,
                        panelWidth - 24, TEXT);
            } else {
                TextRenderUtil.drawClipped(gfx, font, Component.translatable("config.mm.effect." + row.field()),
                        x + 25, rowY + 4, valueX() - x - 31, MUTED);
                if (!row.equals(editingEffect)) {
                    gfx.fill(valueX(), rowY + 1, x + panelWidth - 12, rowY + ROW - 1, 0xFF484B54);
                    TextRenderUtil.drawClipped(gfx, font, Component.literal(effectValue(row)), valueX() + 4,
                            rowY + 4, 112, TEXT);
                }
            }
        }
        gfx.disableScissor();
        drawScrollbar(gfx, rows.size());
    }

    private String effectValue(EffectRow row) {
        WorkingEffectsConfig.Entry entry = WorkingEffectsConfig.get(row.id());
        if (entry == null) return "inherit";
        return switch (row.field()) {
            case "sound" -> entry.sound() == null ? "inherit" : entry.sound();
            case "interval" -> entry.interval() == null ? "inherit" : entry.interval().toString();
            case "particle" -> entry.particle() == null ? "inherit" : entry.particle();
            default -> "inherit";
        };
    }

    private void drawScrollbar(GuiGraphics gfx, int count) {
        if (maxScroll() <= 0 || count == 0) return;
        int track = rowsBottom() - rowsTop();
        int thumb = Math.max(10, track * track / (count * ROW));
        int thumbY = rowsTop() + (track - thumb) * scroll / maxScroll();
        gfx.fill(x + panelWidth - 10, rowsTop(), x + panelWidth - 8, rowsBottom(), 0xFF252525);
        gfx.fill(x + panelWidth - 10, thumbY, x + panelWidth - 8, thumbY + thumb, 0xFFBDBDBD);
    }

    private Component footerText(int mouseX, int mouseY) {
        if (page == Page.EFFECTS) {
            EffectRow hovered = effectRowAt(mouseX, mouseY);
            return hovered != null && hovered.field() != null
                    ? Component.literal(hovered.id() + " · " + hovered.field() + " · inherit = default; empty = off")
                    : Component.translatable("config.mm.effects.hint");
        }
        MMConfigOptions.Option hovered = optionAt(mouseX, mouseY);
        if (hovered == null) return Component.translatable("config.mm.hint");
        String range = hovered.kind() == MMConfigOptions.Kind.INTEGER
                ? "  [" + hovered.min() + ".." + hovered.max() + "]" : "";
        return Component.literal(hovered.key() + range);
    }

    private void button(GuiGraphics gfx, int bx, int by, int bw, int bh, Component label,
                        boolean active, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        gfx.fill(bx, by, bx + bw, by + bh, active ? ACTIVE : hovered && enabled ? 0xFF5A5F69 : 0xFF444750);
        TextRenderUtil.drawClipped(gfx, font, label, bx + 5, by + 4, bw - 10, enabled ? TEXT : MUTED);
    }

    @Nullable
    private MMConfigOptions.Option optionAt(double mouseX, double mouseY) {
        if (page == Page.EFFECTS) return null;
        if (mouseX < x + 8 || mouseX >= x + panelWidth - 8 || mouseY < rowsTop() || mouseY >= rowsBottom()) return null;
        int index = ((int) mouseY - rowsTop() + scroll) / ROW;
        return index >= 0 && index < options().size() ? options().get(index) : null;
    }

    @Nullable
    private EffectRow effectRowAt(double mouseX, double mouseY) {
        if (page != Page.EFFECTS || mouseX < x + 8 || mouseX >= x + panelWidth - 8
                || mouseY < rowsTop() || mouseY >= rowsBottom()) return null;
        List<EffectRow> rows = effectRows();
        int index = ((int) mouseY - rowsTop() + scroll) / ROW;
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (editor != null && editor.isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
        if (!commitEditor()) return true;
        if (mouseY >= y + 7 && mouseY < y + 23 && mouseX >= x + panelWidth - 62 && mouseX < x + panelWidth - 8) {
            onClose();
            return true;
        }
        if (mouseY >= y + 25 && mouseY < y + 41) {
            if (mouseX >= x + 8 && mouseX < x + 98 && canEditServer()) {
                page = Page.SERVER;
                scroll = 0;
                error = null;
                return true;
            }
            if (mouseX >= x + 102 && mouseX < x + 192) {
                page = Page.CLIENT;
                scroll = 0;
                error = null;
                return true;
            }
            if (mouseX >= x + 196 && mouseX < x + 286) {
                page = Page.EFFECTS;
                scroll = 0;
                error = null;
                return true;
            }
        }
        if (page == Page.EFFECTS) {
            EffectRow row = effectRowAt(mouseX, mouseY);
            if (row == null) return true;
            if (row.field() == null) {
                expandedController = row.id().equals(expandedController) ? null : row.id();
                scroll = Mth.clamp(scroll, 0, maxScroll());
                return true;
            }
            if (mouseX < valueX()) return true;
            editingEffect = row;
            int index = effectRows().indexOf(row);
            int rowY = rowsTop() + index * ROW - scroll;
            editor = new EditBox(font, valueX(), rowY + 1, 124, 14, Component.literal(row.field()));
            editor.setMaxLength(128);
            editor.setValue(effectValue(row));
            addRenderableWidget(editor);
            setFocused(editor);
            editor.setFocused(true);
            return true;
        }
        MMConfigOptions.Option option = optionAt(mouseX, mouseY);
        if (option == null || mouseX < valueX() || page == Page.SERVER && serverValues == null) return true;
        if (option.kind() == MMConfigOptions.Kind.BOOLEAN) {
            String current = page == Page.SERVER ? serverValues.get(option.key()) : option.current();
            update(option, String.valueOf(!Boolean.parseBoolean(current)));
        } else if (option.kind() == MMConfigOptions.Kind.BUILD_MODE) {
            String current = serverValues.get(option.key());
            var modes = io.ticticboom.mods.mm.tool.ToolBuildMode.values();
            var mode = io.ticticboom.mods.mm.tool.ToolBuildMode.valueOf(current);
            update(option, modes[(mode.ordinal() + 1) % modes.length].name());
        } else {
            editing = option;
            int row = options().indexOf(option);
            int rowY = rowsTop() + row * ROW - scroll;
            editor = new EditBox(font, valueX(), rowY + 1, 124, 14, Component.literal(option.label()));
            editor.setMaxLength(32);
            editor.setValue(page == Page.SERVER ? serverValues.getOrDefault(option.key(), "") : option.current());
            addRenderableWidget(editor);
            setFocused(editor);
            editor.setFocused(true);
        }
        return true;
    }

    private boolean commitEditor() {
        if (editor == null) return true;
        String text = editor.getValue().strip();
        if (editingEffect != null) {
            if (!WorkingEffectsConfig.validOverride(editingEffect.field(), text)
                    || !WorkingEffectsConfig.setOverride(editingEffect.id(), editingEffect.field(), text)) {
                error = Component.translatable("config.mm.invalid", editingEffect.field());
                return false;
            }
        } else if (editing != null) {
            if (!editing.valid(text)) {
                error = Component.translatable("config.mm.invalid", editing.label());
                return false;
            }
            update(editing, text);
        }
        removeWidget(editor);
        editor = null;
        editing = null;
        editingEffect = null;
        setFocused(null);
        return true;
    }

    private void cancelEditor() {
        if (editor != null) removeWidget(editor);
        editor = null;
        editing = null;
        editingEffect = null;
        error = null;
        setFocused(null);
    }

    private void update(MMConfigOptions.Option option, String value) {
        error = null;
        if (option.scope() == MMConfigOptions.Scope.CLIENT) {
            if (!option.set(value)) error = Component.translatable("config.mm.invalid", option.label());
        } else if (canEditServer()) {
            MMNetwork.INSTANCE.sendToServer(new MMConfigEditPkt(option.key(), value));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < x + 6 || mouseX >= x + panelWidth - 6 || mouseY < rowsTop() || mouseY >= rowsBottom())
            return super.mouseScrolled(mouseX, mouseY, delta);
        if (!commitEditor()) return true;
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * ROW * 2, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editor != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) return commitEditor();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelEditor();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!commitEditor()) return;
        minecraft.setScreen(parent);
    }
}
