package io.ticticboom.mods.mm.structure.layout;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Port slots of a layout. A key marked {@code "allowPort": true} takes a port of the structure in place of its own
 * block. A port key may name, with {@code "replaceWith": "<key>"}, the key whose block goes where its port is left
 * out; without one, a position a port left takes a block of an {@code allowPort} key. How many ports of a port key the
 * structure takes is the key's {@code "minPorts"} to {@code "maxPorts"}. Left out, {@code "maxPorts"} is the number of
 * positions the layout draws of that key, and {@code "minPorts"} is that number too, or 0 when the key has a
 * {@code "replaceWith"}: such a port may be left out, the way multiblocks of other mods let a hatch be.
 * <p>
 * A layout with neither an {@code allowPort} key nor a {@code replaceWith} is checked the classic way.
 */
final class PortSlots {
    /** {@link PortLimit#max()} of a key that takes any number of ports. */
    static final int NO_LIMIT = Integer.MAX_VALUE;

    private final StructureLayout layout;
    /** The layout's port keys, in the order the layout first draws them. */
    private final List<StructureLayoutPiece> portKeys;
    private final Map<StructureLayoutPiece, PortLimit> limits;
    /** The keys a port may stand in for; a position a port left takes a block of any of them. */
    private final List<StructureLayoutPiece> fillers;
    /** A port key's {@code "replaceWith"}: the key whose block goes where its port is left out. */
    private final Map<StructureLayoutPiece, StructureLayoutPiece> replacements;

    private PortSlots(StructureLayout layout, List<StructureLayoutPiece> portKeys, Map<StructureLayoutPiece, PortLimit> limits,
                      List<StructureLayoutPiece> fillers, Map<StructureLayoutPiece, StructureLayoutPiece> replacements) {
        this.layout = layout;
        this.portKeys = portKeys;
        this.limits = limits;
        this.fillers = fillers;
        this.replacements = replacements;
    }

    /** The layout's port slots, or null when no key allows a port and no port key names a replacement. */
    @Nullable
    static PortSlots of(StructureLayout layout, List<PositionedLayoutPiece> positionedPieces,
                        Map<StructureKeyChar, StructureLayoutPiece> keys) {
        var counts = new IdentityHashMap<StructureLayoutPiece, Integer>();
        var portKeys = new ArrayList<StructureLayoutPiece>();
        var fillers = new ArrayList<StructureLayoutPiece>();
        Set<StructureLayoutPiece> seenFillers = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean declared = false;
        for (PositionedLayoutPiece positioned : positionedPieces) {
            StructureLayoutPiece piece = positioned.piece();
            if (piece == null) {
                continue;
            }
            if (StructureLayout.isPortPiece(piece.piece())) {
                if (counts.merge(piece, 1, Integer::sum) == 1) {
                    portKeys.add(piece);
                    declared |= piece.declaredPorts("minPorts").isPresent() || piece.declaredPorts("maxPorts").isPresent();
                }
            } else if (piece.allowsPort() && seenFillers.add(piece)) {
                fillers.add(piece);
            }
        }
        var replacements = new IdentityHashMap<StructureLayoutPiece, StructureLayoutPiece>();
        for (StructureLayoutPiece key : portKeys) {
            StructureLayoutPiece replacement = replacementOf(key, keys);
            if (replacement != null) {
                replacements.put(key, replacement);
            }
        }
        if (fillers.isEmpty() && replacements.isEmpty()) {
            if (declared) {
                Ref.LOG.warn("MM: a structure sets minPorts / maxPorts but has no \"allowPort\" key and no \"replaceWith\", so its ports stay as the layout draws them");
            }
            return null;
        }
        var limits = new IdentityHashMap<StructureLayoutPiece, PortLimit>();
        for (StructureLayoutPiece key : portKeys) {
            limits.put(key, limitOf(key, counts.get(key), replacements.containsKey(key)));
        }
        return new PortSlots(layout, List.copyOf(portKeys), limits, List.copyOf(fillers), replacements);
    }

    /** The key a port key's {@code "replaceWith"} names, or null when it names none, an unknown key or a port key. */
    @Nullable
    private static StructureLayoutPiece replacementOf(StructureLayoutPiece key, Map<StructureKeyChar, StructureLayoutPiece> keys) {
        var json = key.json();
        if (json == null || !json.has("replaceWith")) {
            return null;
        }
        String name;
        try {
            name = json.get("replaceWith").getAsString();
        } catch (RuntimeException e) {
            name = "";
        }
        StructureLayoutPiece replacement = name.length() == 1 ? keys.get(new StructureKeyChar(name.charAt(0))) : null;
        if (replacement == null || StructureLayout.isPortPiece(replacement.piece())) {
            Ref.LOG.error("MM ERROR: \"replaceWith\" of structure key {} must name one other non-port key of the structure, got {}",
                    key.valueId(), json.get("replaceWith"));
            return null;
        }
        return replacement;
    }

    private static PortLimit limitOf(StructureLayoutPiece key, int positions, boolean replaceable) {
        int min = Math.max(0, key.declaredPorts("minPorts").orElse(replaceable ? 0 : positions));
        OptionalInt declaredMax = key.declaredPorts("maxPorts");
        int max = declaredMax.isEmpty() ? Math.max(positions, min)
                : declaredMax.getAsInt() < 0 ? NO_LIMIT : declaredMax.getAsInt();
        if (max < min) {
            Ref.LOG.error("MM ERROR: structure key {} has maxPorts {} below minPorts {}; using {} for both", key.valueId(), max, min, min);
            max = min;
        }
        return new PortLimit(positions, min, max);
    }

    @Nullable
    PortLimit limit(StructureLayoutPiece key) {
        return limits.get(key);
    }

    List<StructureLayoutPiece> fillers() {
        return fillers;
    }

    @Nullable
    StructureLayoutPiece replacement(StructureLayoutPiece key) {
        return replacements.get(key);
    }

    /** What may stand where a port of {@code key} is drawn but left out: its replacement, else the port slot blocks. */
    List<StructureLayoutPiece> replacementsFor(StructureLayoutPiece key) {
        StructureLayoutPiece replacement = replacements.get(key);
        return replacement != null ? List.of(replacement) : fillers;
    }

    /** Where a port of {@code key} is drawn but left out: its first replacement, or the port itself when there is none. */
    private PositionedLayoutPiece emptied(PositionedLayoutPiece piece) {
        List<StructureLayoutPiece> options = replacementsFor(piece.piece());
        return options.isEmpty() ? piece : new PositionedLayoutPiece(piece.pos(), options.get(0));
    }

    /**
     * The pieces that keep this rotation from forming, empty when it is formed. With {@code firstOnly} the search
     * stops at the first reason, which is all a formed check needs.
     */
    List<PositionedLayoutPiece> missing(Level level, BlockPos controllerPos, StructureModel model,
                                        List<PositionedLayoutPiece> positioned, Rotation rot, boolean firstOnly) {
        var missing = new ArrayList<PositionedLayoutPiece>();
        var holders = new ArrayList<PositionedLayoutPiece>(); // positions with a port on them
        var freed = new ArrayList<PositionedLayoutPiece>();   // port positions without one
        Set<PositionedLayoutPiece> reported = Collections.newSetFromMap(new IdentityHashMap<>());
        int helpers = 0;
        HelperSlotOverrides.Rule rule = StructureLayout.helperRule(level, model);
        for (PositionedLayoutPiece piece : positioned) {
            BlockPos pos = piece.findAbsolutePos(controllerPos);
            if (StructureLayout.isPortPiece(piece.piece().piece())) {
                (portAt(level, pos) ? holders : freed).add(piece);
            } else if (piece.formed(level, controllerPos, model, rot)
                    || (StructureLayout.isHelperInPlace(level, controllerPos, piece, rule) && layout.helperFits(rule, ++helpers))) {
                continue;
            } else if (piece.piece().allowsPort() && portAt(level, pos)) {
                holders.add(piece);
            } else {
                missing.add(piece);
                reported.add(piece);
                if (firstOnly) {
                    return missing;
                }
            }
        }

        // a position a port left takes its key's replacement, or a block of a key that allows ports
        var unfilled = new ArrayList<PositionedLayoutPiece>();
        for (PositionedLayoutPiece piece : freed) {
            if (!anyAt(replacementsFor(piece.piece()), level, piece.findAbsolutePos(controllerPos), model, rot)) {
                if (firstOnly) {
                    return List.of(piece);
                }
                unfilled.add(piece);
            }
        }

        int ports = holders.size();
        int keys = portKeys.size();
        boolean[][] fits = new boolean[ports][keys];
        for (int p = 0; p < ports; p++) {
            PositionedLayoutPiece holder = holders.get(p);
            BlockPos pos = holder.findAbsolutePos(controllerPos);
            for (int k = 0; k < keys; k++) {
                StructureLayoutPiece key = portKeys.get(k);
                fits[p][k] = mayStandAt(key, holder.piece()) && isPortOf(key, level, pos, rot, model);
            }
        }

        // Each port has to count for a key, no key for more than its maximum, every key for its minimum. Those are
        // two matchings over the same ports: one that places every port, one that fills every minimum. Where both
        // exist, a single assignment doing both exists too (Mendelsohn-Dulmage), so they are checked one by one.
        int[] keyOfColumn = columns(ports, false);
        int[] columnOfPort = matching(spread(fits, keyOfColumn, false), ports, keyOfColumn.length);
        for (int p = 0; p < ports; p++) {
            PositionedLayoutPiece holder = holders.get(p);
            if (columnOfPort[p] < 0 && reported.add(holder)) {
                // A port no key takes. On a port position of its own kind it is one more than the key's maximum,
                // so the block belongs there; anywhere else the piece drawn there says what does.
                int own = portKeys.indexOf(holder.piece());
                boolean surplus = own >= 0 && fits[p][own];
                missing.add(surplus ? emptied(holder) : holder);
                if (firstOnly) {
                    return missing;
                }
            }
        }

        int[] keyOfRow = columns(ports, true);
        int[] portOfRow = matching(spread(fits, keyOfRow, true), keyOfRow.length, ports);
        int[] lacking = new int[keys];
        for (int row = 0; row < keyOfRow.length; row++) {
            if (portOfRow[row] < 0) {
                lacking[keyOfRow[row]]++;
            }
        }
        for (int k = 0; k < keys; k++) {
            if (lacking[k] == 0) {
                continue;
            }
            StructureLayoutPiece key = portKeys.get(k);
            if (firstOnly) {
                return List.of(firstPositionOf(key, positioned));
            }
            // point at as many places as the key is short of ports: its own empty positions first, then free slots
            int pointed = 0;
            for (PositionedLayoutPiece piece : freed) {
                if (pointed < lacking[k] && piece.piece() == key && reported.add(piece)) {
                    missing.add(piece);
                    pointed++;
                }
            }
            for (PositionedLayoutPiece piece : positioned) {
                if (pointed < lacking[k] && piece.piece().allowsPort() && !StructureLayout.isPortPiece(piece.piece().piece())
                        && !holders.contains(piece) && reported.add(piece)) {
                    missing.add(new PositionedLayoutPiece(piece.pos(), key));
                    pointed++;
                }
            }
            if (pointed == 0) {
                missing.add(firstPositionOf(key, positioned));
            }
        }

        for (PositionedLayoutPiece piece : unfilled) {
            if (reported.add(piece)) {
                // its key has ports enough, so what belongs here is the replacement block
                missing.add(emptied(piece));
            }
        }
        return missing;
    }

    /**
     * Where a port counting for {@code key} may stand: on any port slot, on a position of its own key, and for a
     * port that may go anywhere on every position of such ports.
     */
    private boolean mayStandAt(StructureLayoutPiece key, StructureLayoutPiece at) {
        if (!StructureLayout.isPortPiece(at.piece()) || at == key) {
            return true;
        }
        return layout.isAnywhere(key.piece()) && layout.isAnywhere(at.piece());
    }

    /** Whether the port at {@code pos} is one {@code key} asks for, the key's modifiers included. */
    private boolean isPortOf(StructureLayoutPiece key, Level level, BlockPos pos, Rotation rot, StructureModel model) {
        if (layout.isAnywhere(key.piece())) {
            return layout.candidateMatches(key, key.piece(), level, pos, rot, model);
        }
        return key.formed(level, pos, model, rot);
    }

    private static boolean anyAt(List<StructureLayoutPiece> options, Level level, BlockPos pos, StructureModel model, Rotation rot) {
        for (StructureLayoutPiece option : options) {
            if (option.formed(level, pos, model, rot)) {
                return true;
            }
        }
        return false;
    }

    private static boolean portAt(Level level, BlockPos pos) {
        var be = level.getExistingBlockEntity(pos);
        if (be == null && level instanceof ServerLevel serverLevel) {
            be = WorldUtil.getBlockEntity(pos, serverLevel);
        }
        return be instanceof IPortBlockEntity;
    }

    private static PositionedLayoutPiece firstPositionOf(StructureLayoutPiece key, List<PositionedLayoutPiece> positioned) {
        for (PositionedLayoutPiece piece : positioned) {
            if (piece.piece() == key) {
                return piece;
            }
        }
        throw new IllegalStateException("structure key " + key.valueId() + " is not in its own layout");
    }

    /**
     * One entry per port a key takes, holding the key's index: up to its maximum, or with {@code required} only its
     * minimum. No key is given more entries than there are ports, so "no limit" stays small.
     */
    private int[] columns(int ports, boolean required) {
        int[] result = new int[0];
        for (int k = 0; k < portKeys.size(); k++) {
            PortLimit limit = limits.get(portKeys.get(k));
            int count = required ? limit.min() : Math.min(limit.max(), ports);
            int from = result.length;
            result = Arrays.copyOf(result, from + count);
            Arrays.fill(result, from, from + count, k);
        }
        return result;
    }

    /** {@code fits} with a key's column repeated per entry of {@code keyOf}; {@code transposed} puts the keys in the rows. */
    private static boolean[][] spread(boolean[][] fits, int[] keyOf, boolean transposed) {
        int ports = fits.length;
        boolean[][] result = transposed ? new boolean[keyOf.length][ports] : new boolean[ports][keyOf.length];
        for (int p = 0; p < ports; p++) {
            for (int i = 0; i < keyOf.length; i++) {
                if (fits[p][keyOf[i]]) {
                    if (transposed) {
                        result[i][p] = true;
                    } else {
                        result[p][i] = true;
                    }
                }
            }
        }
        return result;
    }

    /** A maximum matching of rows to columns (augmenting paths): the column each row got, or -1. */
    private static int[] matching(boolean[][] adj, int rows, int cols) {
        int[] rowOf = new int[cols];
        int[] colOf = new int[rows];
        Arrays.fill(rowOf, -1);
        Arrays.fill(colOf, -1);
        for (int row = 0; row < rows; row++) {
            augment(row, adj, rowOf, colOf, new boolean[cols]);
        }
        return colOf;
    }

    private static boolean augment(int row, boolean[][] adj, int[] rowOf, int[] colOf, boolean[] seen) {
        for (int col = 0; col < rowOf.length; col++) {
            if (!adj[row][col] || seen[col]) {
                continue;
            }
            seen[col] = true;
            if (rowOf[col] < 0 || augment(rowOf[col], adj, rowOf, colOf, seen)) {
                rowOf[col] = row;
                colOf[row] = col;
                return true;
            }
        }
        return false;
    }
}
