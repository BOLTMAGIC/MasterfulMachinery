package io.ticticboom.mods.mm.client.builder;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.TierResolver;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.AssemblyPkt;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Pick the structure (when there are several) and a port tier per port type, then assemble. */
public class AssemblyScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW = 16;
    private static final int PANEL = 0xE01D1F24;

    private final Screen parent;
    private final MachineControllerBlockEntity be;
    private final TierPrefs prefs = new TierPrefs();
    private StructureModel structure;
    private StructureTierRows tierRows = new StructureTierRows(null);
    private int left;
    private int top;

    public AssemblyScreen(Screen parent, MachineControllerBlockEntity be) {
        super(Component.translatable("gui.mm.assemble.title"));
        this.parent = parent;
        this.be = be;
        this.prefs.copyFrom(be.getAssemblyTiers());
        this.structure = be.getAssemblyStructure();
    }

    @Override
    protected void init() {
        collectRows();
        int height = 44 + tierRows.rows().size() * ROW + 24;
        left = (this.width - WIDTH) / 2;
        top = (this.height - height) / 2;
        int y = top + 22;

        List<StructureModel> candidates = be.getAssemblyCandidates();
        if (candidates.size() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleStructure(-1)).bounds(left + WIDTH - 44, y - 2, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleStructure(1)).bounds(left + WIDTH - 22, y - 2, 14, 14).build());
        }
        y += ROW + 4;
        for (String key : tierRows.rows().keySet()) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleTier(key, -1)).bounds(left + WIDTH - 44, y - 2, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleTier(key, 1)).bounds(left + WIDTH - 22, y - 2, 14, 14).build());
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.assemble.start"), b -> start())
                .bounds(left + WIDTH / 2 - 40, y + 4, 80, 18).build());
    }

    private void collectRows() {
        tierRows = new StructureTierRows(structure);
    }

    private void cycleStructure(int step) {
        List<StructureModel> candidates = be.getAssemblyCandidates();
        int index = Math.max(0, candidates.indexOf(structure));
        structure = candidates.get(Math.floorMod(index + step, candidates.size()));
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.SET_STRUCTURE, structure.id().toString(), 0));
        rebuildWidgets();
    }

    private void cycleTier(String key, int step) {
        // "lowest" followed by every tier this structure accepts for the port type
        int next = TierPrefs.cycle(prefs.get(key), tierRows.rows().get(key).keySet(), step);
        prefs.set(key, next);
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.SET_TIER, key, next));
    }

    private void start() {
        String shown = structure == null ? "" : structure.id().toString();
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.START, shown, 0));
        onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        int height = 44 + tierRows.rows().size() * ROW + 24;
        gfx.fill(left, top, left + WIDTH, top + height, PANEL);
        gfx.drawString(font, this.title, left + 8, top + 7, 0xFFFFFF, false);

        int y = top + 22;
        Component name = structure == null ? Component.translatable("message.mm.assemble.no_structure") : Component.literal(structure.name());
        gfx.drawString(font, name, left + 8, y + 1, 0xE0E0E0, false);
        y += ROW + 4;
        boolean anyAdjusted = false;
        for (String key : tierRows.rows().keySet()) {
            int preferred = prefs.get(key);
            Block block = tierRows.chosenBlock(key, preferred);
            Component label = block == null ? Component.literal("?") : block.getName();
            if (preferred == TierResolver.LOWEST) {
                label = Component.translatable("gui.mm.assemble.lowest", label);
            }
            boolean adjusted = tierRows.adjusted(key, preferred);
            anyAdjusted |= adjusted;
            gfx.drawString(font, font.plainSubstrByWidth(label.getString(), WIDTH - 60) + (adjusted ? " *" : ""),
                    left + 8, y + 1, adjusted ? 0xFFD84D : 0xE0E0E0, false);
            y += ROW;
        }
        if (anyAdjusted) {
            gfx.drawString(font, Component.translatable("gui.mm.assemble.adjusted").withStyle(ChatFormatting.GRAY), left + 8, y - 2, 0x9A9A9A, false);
        }
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
