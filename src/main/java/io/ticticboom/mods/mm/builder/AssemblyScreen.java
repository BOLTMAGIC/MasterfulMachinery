package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.AssemblyPkt;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pick the structure (when there are several) and a port tier per port type, then assemble. */
public class AssemblyScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW = 16;
    private static final int PANEL = 0xE01D1F24;

    private final Screen parent;
    private final MachineControllerBlockEntity be;
    private final TierPrefs prefs = new TierPrefs();
    private StructureModel structure;
    /** port type key -> tier -> block, over every port_type position of the structure */
    private final Map<String, TreeMap<Integer, Block>> rows = new LinkedHashMap<>();
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
        int height = 44 + rows.size() * ROW + 24;
        left = (this.width - WIDTH) / 2;
        top = (this.height - height) / 2;
        int y = top + 22;

        List<StructureModel> candidates = be.getAssemblyCandidates();
        if (candidates.size() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleStructure(-1)).bounds(left + WIDTH - 44, y - 2, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleStructure(1)).bounds(left + WIDTH - 22, y - 2, 14, 14).build());
        }
        y += ROW + 4;
        for (String key : rows.keySet()) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleTier(key, -1)).bounds(left + WIDTH - 44, y - 2, 14, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleTier(key, 1)).bounds(left + WIDTH - 22, y - 2, 14, 14).build());
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.assemble.start"), b -> start())
                .bounds(left + WIDTH / 2 - 40, y + 4, 80, 18).build());
    }

    private void collectRows() {
        rows.clear();
        if (structure == null) {
            return;
        }
        for (var positioned : structure.layout().getPositionedPieces()) {
            if (positioned.piece().piece() instanceof PortTypeStructurePiece portType) {
                String key = PortTiers.key(portType.getPortTypeId(), portType.getInput().orElse(true));
                var tiers = rows.computeIfAbsent(key, k -> new TreeMap<>());
                portType.getBlocksByRank()
                        .subMap(portType.getMinTier(), true, portType.getMaxTier(), true)
                        .forEach(tiers::putIfAbsent);
            }
        }
        rows.values().removeIf(Map::isEmpty);
    }

    private void cycleStructure(int step) {
        List<StructureModel> candidates = be.getAssemblyCandidates();
        int index = Math.max(0, candidates.indexOf(structure));
        structure = candidates.get(Math.floorMod(index + step, candidates.size()));
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.SET_STRUCTURE, structure.id().toString(), 0));
        rebuildWidgets();
    }

    private void cycleTier(String key, int step) {
        // options: "lowest" followed by every tier this structure accepts for the port type
        List<Integer> options = new ArrayList<>();
        options.add(TierResolver.LOWEST);
        options.addAll(rows.get(key).keySet());
        int index = Math.max(0, options.indexOf(prefs.get(key)));
        int next = options.get(Math.floorMod(index + step, options.size()));
        prefs.set(key, next);
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.SET_TIER, key, next));
    }

    private void start() {
        MMNetwork.INSTANCE.sendToServer(new AssemblyPkt(be.getBlockPos(), AssemblyPkt.Action.START, "", 0));
        onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        int height = 44 + rows.size() * ROW + 24;
        gfx.fill(left, top, left + WIDTH, top + height, PANEL);
        gfx.drawString(font, this.title, left + 8, top + 7, 0xFFFFFF, false);

        int y = top + 22;
        Component name = structure == null ? Component.translatable("message.mm.assemble.no_structure") : Component.literal(structure.name());
        gfx.drawString(font, name, left + 8, y + 1, 0xE0E0E0, false);
        y += ROW + 4;
        boolean anyAdjusted = false;
        for (var entry : rows.entrySet()) {
            int preferred = prefs.get(entry.getKey());
            int chosen = TierResolver.resolve(preferred, Integer.MIN_VALUE, Integer.MAX_VALUE, entry.getValue().navigableKeySet());
            Block block = entry.getValue().get(chosen);
            Component label = block == null ? Component.literal("?") : block.getName();
            if (preferred == TierResolver.LOWEST) {
                label = Component.translatable("gui.mm.assemble.lowest", label);
            }
            boolean adjusted = preferred != TierResolver.LOWEST && chosen != preferred;
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
