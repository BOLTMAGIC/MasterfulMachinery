package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;

import java.util.ArrayList;
import java.util.List;

public record SingleMachineLayout(List<Group> groups, int barX, int barY, int width, int height,
                                  int panelBottom, int inventoryX, int inventoryY) {

    public static final int MIN_WIDTH = 174;
    public static final int FRAME = 6;
    public static final int LEFT = 10;
    public static final int RIGHT_MARGIN = 11;
    public static final int NAME_Y = 10;
    public static final int STATUS_Y = 22;
    public static final int CELLS_Y = 38;
    public static final int CELL = 18;
    public static final int GAUGE_W = 12;
    public static final int GAUGE_H = 54;
    public static final int GAUGE_GAP = 2;
    public static final int GROUP_GAP = 4;
    public static final int BAR_W = 24;
    public static final int BAR_H = 5;
    public static final int BAR_GAP = 6;
    public static final int INVENTORY_W = 162;
    public static final int INVENTORY_H = 76;
    public static final int PANEL_GAP = 12;
    private static final int DEFAULT_INVENTORY_Y = 141;

    public record Group(int index, IPortStorage storage, boolean input, boolean items, int x, int y, int columns, int rows, int gauges, int gaugeH) {

        public int width() {
            return items ? columns * CELL : gauges * GAUGE_W + (gauges - 1) * GAUGE_GAP;
        }

        public int height() {
            return items ? rows * CELL : gaugeH;
        }

        public int cellX(int cell) {
            return x + (cell % columns) * CELL;
        }

        public int cellY(int cell) {
            return y + (cell / columns) * CELL;
        }

        public int gaugeX(int gauge) {
            return x + gauge * (GAUGE_W + GAUGE_GAP);
        }

        public int cells() {
            return items ? ((ItemPortStorage) storage).getHandler().getSlots() : gauges;
        }

        private Group at(int newX, int newY) {
            return new Group(index, storage, input, items, newX, newY, columns, rows, gauges, gaugeH);
        }

        private Group reshaped(int newColumns) {
            return new Group(index, storage, input, items, x, y, newColumns, (cells() + newColumns - 1) / newColumns, gauges, gaugeH);
        }
    }

    public int inventoryXOffset() {
        return inventoryX + 1 - 8;
    }

    public int inventoryYOffset() {
        return inventoryY + 1 - DEFAULT_INVENTORY_Y;
    }

    public static SingleMachineLayout of(SingleMachineBlockEntity machine) {
        var storages = machine.getStorages();
        var slots = machine.getSlots();
        var inputs = new ArrayList<Group>();
        var outputs = new ArrayList<Group>();
        for (int i = 0; i < storages.size(); i++) {
            (slots.get(i).input() ? inputs : outputs).add(measure(i, storages.get(i), slots.get(i).input()));
        }
        var leftUnits = units(inputs, false);
        var rightUnits = units(outputs, true);
        int rowH = 0;
        for (Unit unit : leftUnits) rowH = Math.max(rowH, unit.height());
        for (Unit unit : rightUnits) rowH = Math.max(rowH, unit.height());
        var leftColumns = columns(leftUnits, rowH);
        var rightColumns = columns(rightUnits, rowH);

        int leftW = columnsWidth(leftColumns);
        int content = leftW + BAR_GAP + BAR_W + BAR_GAP + columnsWidth(rightColumns);
        int width = Math.max(MIN_WIDTH, LEFT + content + RIGHT_MARGIN);
        int startX = LEFT + (width - LEFT - RIGHT_MARGIN - content) / 2;
        int barX = startX + leftW + BAR_GAP;
        int centerY = CELLS_Y + rowH / 2;

        var placed = new ArrayList<Group>();
        int x = startX;
        for (Column column : leftColumns) {
            column.place(x, rowH, placed);
            x += column.width + GROUP_GAP;
        }
        x = startX + content;
        for (Column column : rightColumns) {
            x -= column.width;
            column.place(x, rowH, placed);
            x -= GROUP_GAP;
        }
        int panelBottom = CELLS_Y + rowH + 8;
        int inventoryY = panelBottom + PANEL_GAP;
        int height = inventoryY + INVENTORY_H + 7;
        return new SingleMachineLayout(List.copyOf(placed), barX, centerY - BAR_H / 2, width, height,
                panelBottom, (width - INVENTORY_W) / 2, inventoryY);
    }

    private record Unit(List<Group> groups) {

        int width() {
            return rowWidth(groups);
        }

        int height() {
            int tallest = 0;
            for (Group group : groups) tallest = Math.max(tallest, group.height());
            return tallest;
        }
    }

    private static final class Column {
        private final List<Unit> units = new ArrayList<>();
        private final int width;
        private int used;

        private Column(Unit first) {
            units.add(first);
            width = first.width();
            used = first.height();
        }

        private Unit fit(Unit unit, int rowH) {
            if (unit.groups().size() == 1 && unit.groups().get(0).items()) {
                var grid = unit.groups().get(0);
                for (int columns = Math.min(grid.cells(), width / CELL); columns >= 1; columns--) {
                    var shaped = new Unit(List.of(grid.reshaped(columns)));
                    if (shaped.height() + used + GROUP_GAP <= rowH) {
                        return shaped;
                    }
                }
                return null;
            }
            return unit.width() <= width && used + GROUP_GAP + unit.height() <= rowH ? unit : null;
        }

        private void add(Unit unit) {
            units.add(unit);
            used += GROUP_GAP + unit.height();
        }

        private void place(int x, int rowH, List<Group> placed) {
            int y = CELLS_Y + (rowH - used) / 2;
            for (Unit unit : units) {
                int gx = x + (width - unit.width()) / 2;
                for (Group group : unit.groups()) {
                    placed.add(group.at(gx, y + (unit.height() - group.height()) / 2));
                    gx += group.width() + GROUP_GAP;
                }
                y += unit.height() + GROUP_GAP;
            }
        }
    }

    private static List<Unit> units(List<Group> side, boolean outputs) {
        var units = new ArrayList<Unit>();
        var bars = new ArrayList<Group>();
        if (outputs) {
            bars.addAll(ofKind(side, Kind.TANK));
            bars.addAll(ofKind(side, Kind.ENERGY));
        } else {
            bars.addAll(ofKind(side, Kind.ENERGY));
            bars.addAll(ofKind(side, Kind.TANK));
        }
        if (!bars.isEmpty()) {
            units.add(new Unit(bars));
        }
        var items = ofKind(side, Kind.ITEMS);
        for (Group grid : outputs ? items.reversed() : items) {
            units.add(new Unit(List.of(grid)));
        }
        return units;
    }

    private static List<Column> columns(List<Unit> units, int rowH) {
        var columns = new ArrayList<Column>();
        for (Unit unit : units) {
            boolean stacked = false;
            for (Column column : columns) {
                var fitted = column.fit(unit, rowH);
                if (fitted != null) {
                    column.add(fitted);
                    stacked = true;
                    break;
                }
            }
            if (!stacked) {
                columns.add(new Column(unit));
            }
        }
        return columns;
    }

    private static int columnsWidth(List<Column> columns) {
        int total = 0;
        for (Column column : columns) total += column.width;
        return total + Math.max(0, columns.size() - 1) * GROUP_GAP;
    }

    private enum Kind { ENERGY, TANK, ITEMS }

    private static Kind kind(Group group) {
        if (group.items()) return Kind.ITEMS;
        return group.storage() instanceof EnergyPortStorage ? Kind.ENERGY : Kind.TANK;
    }

    private static List<Group> ofKind(List<Group> groups, Kind kind) {
        return groups.stream().filter(g -> kind(g) == kind).toList();
    }

    private static int rowWidth(List<Group> groups) {
        int total = 0;
        for (Group group : groups) total += group.width();
        return total + Math.max(0, groups.size() - 1) * GROUP_GAP;
    }

    private static Group measure(int index, IPortStorage storage, boolean input) {
        if (storage instanceof ItemPortStorage items) {
            var model = (ItemPortStorageModel) items.getStorageModel();
            return new Group(index, storage, input, true, 0, 0, model.columns(), model.rows(), 0, 0);
        }
        return new Group(index, storage, input, false, 0, 0, 0, 0, Math.max(1, storage.contents().size()), GAUGE_H);
    }
}
