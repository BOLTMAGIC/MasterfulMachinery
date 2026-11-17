package io.ticticboom.mods.mm.structure.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.port.PortAnywhereStructurePiece;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeAnywhereStructurePiece;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WorldUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public class StructureLayout {
    @Getter
    private StructureCharacterGrid charGrid;

    @Getter
    private List<PositionedLayoutPiece> positionedPieces = new ArrayList<>();

    @Getter
    Map<Rotation, List<PositionedLayoutPiece>> rotatedPositionedPieces = new HashMap<>();

    @Getter
    private Map<StructureKeyChar, StructureLayoutPiece> pieces;

    @Getter
    private boolean portsAnywhereGlobal;

    /** How many Input Gateways / Pattern Ports may stand in for pieces at once; negative means no limit. */
    @Getter
    private final int maxHelpers;

    /** Set when a key of the layout allows a port in its place; the layout is then checked by {@link PortSlots}. */
    @Nullable
    private final PortSlots portSlots;

    public StructureLayout(StructureCharacterGrid rawLayout, Map<StructureKeyChar, StructureLayoutPiece> pieces, boolean portsAnywhereGlobal, int maxHelpers) {
        this.charGrid = rawLayout;
        this.pieces = pieces;
        this.portsAnywhereGlobal = portsAnywhereGlobal;
        this.maxHelpers = maxHelpers;
        setupVariants();
        this.portSlots = PortSlots.of(this, positionedPieces, pieces);
    }

    public StructureLayout(StructureCharacterGrid rawLayout, Map<StructureKeyChar, StructureLayoutPiece> pieces, boolean portsAnywhereGlobal) {
        this(rawLayout, pieces, portsAnywhereGlobal, -1);
    }

    // Backwards-compatible constructor
    public StructureLayout(StructureCharacterGrid rawLayout, Map<StructureKeyChar, StructureLayoutPiece> pieces) {
        this(rawLayout, pieces, false);
    }

    public void setupVariants() {
        charGrid.runFor((pos, cPos, chr) -> {
            StructureLayoutPiece piece = pieces.get(chr);
            positionedPieces.add(new PositionedLayoutPiece(pos.subtract(cPos), piece));
        });

        rotatedPositionedPieces.put(Rotation.NONE, positionedPieces);

        for (Rotation rotation : Rotation.values()) {
            var rotatedPieces = new ArrayList<PositionedLayoutPiece>();
            for (PositionedLayoutPiece piece : positionedPieces) {
                rotatedPieces.add(piece.rotate(rotation));
            }
            rotatedPositionedPieces.put(rotation, rotatedPieces);
        }
    }

    public boolean formed(Level level, BlockPos worldControllerPos, StructureModel model) {
        for (var entry : rotatedPositionedPieces.entrySet()) {
            if (innerFormed(level, worldControllerPos, model, entry.getValue(), entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The pieces that keep the structure from forming, for the rotation where that list is shortest.
     * A port that may go anywhere counts as missing when no port position can hold it.
     */
    public List<PositionedLayoutPiece> closestMissing(Level level, BlockPos worldControllerPos, StructureModel model) {
        List<PositionedLayoutPiece> best = null;
        for (var entry : rotatedPositionedPieces.entrySet()) {
            var missing = missingPieces(level, worldControllerPos, model, entry.getValue(), entry.getKey());
            if (best == null || missing.size() < best.size()) {
                best = missing;
            }
        }
        return best == null ? List.of() : best;
    }

    private List<PositionedLayoutPiece> missingPieces(Level level, BlockPos worldControllerPos, StructureModel model, List<PositionedLayoutPiece> positionedPieces, Rotation rot) {
        if (portSlots != null) {
            return portSlots.missing(level, worldControllerPos, model, positionedPieces, rot, false);
        }
        var missing = new ArrayList<PositionedLayoutPiece>();
        var anywherePieces = new ArrayList<PositionedLayoutPiece>();
        int helpers = 0;
        HelperSlotOverrides.Rule rule = helperRule(level, model);
        for (PositionedLayoutPiece piece : positionedPieces) {
            if (isAnywhere(piece.piece().piece())) {
                anywherePieces.add(piece);
            } else if (!piece.formed(level, worldControllerPos, model, rot)) {
                if (isHelperInPlace(level, worldControllerPos, piece, rule) && helperFits(rule, ++helpers)) {
                    continue;
                }
                missing.add(piece);
            }
        }
        for (PositionedLayoutPiece req : anywherePieces) {
            boolean placeable = false;
            for (PositionedLayoutPiece candidate : anywherePieces) {
                if (candidateMatches(req.piece(), req.piece().piece(), level, candidate.findAbsolutePos(worldControllerPos), rot, model)) {
                    placeable = true;
                    break;
                }
            }
            if (!placeable) {
                missing.add(req);
            }
        }
        return missing;
    }

    /** Whether the piece is a port matched against any port position of the structure, not only its own. */
    public boolean isAnywhere(Object underlying) {
        return underlying instanceof PortAnywhereStructurePiece || underlying instanceof PortTypeAnywhereStructurePiece
                || (portsAnywhereGlobal && (underlying instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece
                || underlying instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece));
    }

    public JsonObject debugFormed(Level level, BlockPos worldControllerPos, StructureModel model) {
        var json = new JsonObject();
        for (var entry : rotatedPositionedPieces.entrySet()) {
            json.add(entry.getKey().name(), debugInnerFormed(level, worldControllerPos, model, entry.getValue(), entry.getKey()));
        }
        return json;
    }

    public JsonObject debugInnerFormed(Level level, BlockPos worldControllerPos, StructureModel model, List<PositionedLayoutPiece> positionedPieces, Rotation rot) {
        var json = new JsonObject();
        json.addProperty("formed", innerFormed(level, worldControllerPos, model, positionedPieces, rot));
        json.addProperty("rotation", rot.name());
        var piecesJson = new JsonArray();
        for (PositionedLayoutPiece positionedPiece : positionedPieces) {
            var pieceJson = positionedPiece.piece().debugFormed(level, worldControllerPos, model, rot);
            piecesJson.add(pieceJson);
        }
        json.add("pieces", piecesJson);
        return json;
    }

    public void validate(StructurePieceSetupMetadata meta) {
        for (StructureLayoutPiece value : pieces.values()) {
            value.validate(meta);
        }
    }

    private boolean innerFormed(Level level, BlockPos worldControllerPos, StructureModel model, List<PositionedLayoutPiece> positionedPieces, Rotation rot) {
        if (portSlots != null) {
            return portSlots.missing(level, worldControllerPos, model, positionedPieces, rot, true).isEmpty();
        }
        // First pass: validate all non-anywhere pieces in-place
        List<PositionedLayoutPiece> anywherePieces = new ArrayList<>();
        int helpers = 0;
        HelperSlotOverrides.Rule rule = helperRule(level, model);
        for (PositionedLayoutPiece piece : positionedPieces) {
            var layoutPiece = piece.piece();
            var underlying = layoutPiece.piece();
            // treat port pieces as anywhere if either they are explicitly anywhere OR the layout enables portsAnywhereGlobal
            if (underlying instanceof PortAnywhereStructurePiece || underlying instanceof PortTypeAnywhereStructurePiece
                    || (portsAnywhereGlobal && (underlying instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece || underlying instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece))) {
                anywherePieces.add(piece);
                continue; // skip per-position check for anywhere pieces
            }
            if (!piece.formed(level, worldControllerPos, model, rot)
                    && !(isHelperInPlace(level, worldControllerPos, piece, rule) && helperFits(rule, ++helpers))) {
                return false;
            }
        }

        // If there are no anywhere pieces, we're done
        if (anywherePieces.isEmpty()) return true;

        // Build candidate positions for anywhere pieces (absolute positions in this rotation)
        List<BlockPos> candidatePositions = new ArrayList<>();
        for (PositionedLayoutPiece p : anywherePieces) {
            candidatePositions.add(p.findAbsolutePos(worldControllerPos));
        }

        // For matching we need to ensure each anywhere-piece's requirement can be satisfied by a unique candidate position
        int reqCount = anywherePieces.size();
        int candCount = candidatePositions.size();
        if (candCount < reqCount) return false;

        // Precompute which candidate positions satisfy which requirements (including modifiers)
        boolean[][] ok = new boolean[reqCount][candCount];
        for (int i = 0; i < reqCount; i++) {
            var layoutReq = anywherePieces.get(i).piece();
            var reqPiece = layoutReq.piece();
            for (int j = 0; j < candCount; j++) {
                BlockPos abs = candidatePositions.get(j);
                ok[i][j] = candidateMatches(layoutReq, reqPiece, level, abs, rot, model);
            }
        }

        // Use Hopcroft-Karp to find maximum bipartite matching (left=reqCount, right=candCount)
        return hopcroftKarp(ok, reqCount, candCount);
    }

    private boolean hopcroftKarp(boolean[][] adj, int nU, int nV) {
        final int INF = Integer.MAX_VALUE / 4;
        int[] pairU = new int[nU];
        int[] pairV = new int[nV];
        int[] dist = new int[nU];
        java.util.Arrays.fill(pairU, -1);
        java.util.Arrays.fill(pairV, -1);

        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();

        while (true) {
            // BFS layer build
            queue.clear();
            for (int u = 0; u < nU; u++) {
                if (pairU[u] == -1) {
                    dist[u] = 0;
                    queue.add(u);
                } else {
                    dist[u] = INF;
                }
            }
            int reachable = INF;
            while (!queue.isEmpty()) {
                int u = queue.poll();
                if (dist[u] >= reachable) continue;
                for (int v = 0; v < nV; v++) {
                    if (!adj[u][v]) continue;
                    int pu = pairV[v];
                    if (pu == -1) {
                        reachable = dist[u] + 1;
                    } else if (dist[pu] == INF) {
                        dist[pu] = dist[u] + 1;
                        queue.add(pu);
                    }
                }
            }

            if (reachable == INF) break; // no more augmenting paths

            // DFS to find augmenting paths
            boolean progress = false;
            for (int u = 0; u < nU; u++) {
                if (pairU[u] == -1) {
                    if (dfsHK(u, adj, pairU, pairV, dist)) {
                        progress = true;
                    }
                }
            }
            if (!progress) break;
        }

        // check if all left vertices matched
        for (int u = 0; u < nU; u++) {
            if (pairU[u] == -1) return false;
        }
        return true;
    }

    private boolean dfsHK(int u, boolean[][] adj, int[] pairU, int[] pairV, int[] dist) {
        final int nV = pairV.length;
        for (int v = 0; v < nV; v++) {
            if (!adj[u][v]) continue;
            int pu = pairV[v];
            if (pu == -1 || (dist[pu] == dist[u] + 1 && dfsHK(pu, adj, pairU, pairV, dist))) {
                pairU[u] = v;
                pairV[v] = u;
                return true;
            }
        }
        dist[u] = Integer.MAX_VALUE / 4; // mark as visited in this phase
        return false;
    }

    boolean candidateMatches(StructureLayoutPiece layoutReq, Object requiredPiece, Level level, BlockPos absPos, Rotation rot, StructureModel model) {
        // get block entity safely
        var be = level.getExistingBlockEntity(absPos);
        if (be == null) {
            try {
                be = WorldUtil.getBlockEntity(absPos, (ServerLevel) level);
            } catch (Exception ignored) {
                return false;
            }
        }
        if (!(be instanceof IPortBlockEntity pbe)) return false;

        // handle both explicit anywhere pieces and normal port pieces when layout/global flag marks them anywhere
        boolean portMatch = false;
        if (requiredPiece instanceof PortAnywhereStructurePiece || requiredPiece instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece) {
            PortAnywhereStructurePiece pa = null;
            io.ticticboom.mods.mm.piece.type.port.PortStructurePiece normalP = null;
            if (requiredPiece instanceof PortAnywhereStructurePiece) pa = (PortAnywhereStructurePiece) requiredPiece;
            if (requiredPiece instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece) normalP = (io.ticticboom.mods.mm.piece.type.port.PortStructurePiece) requiredPiece;
            var pm = pbe.getModel();
            String expectedPath = pa != null ? pa.getPortId().getPath() : normalP.getPortId().getPath();
            Optional<Boolean> expectedInputOpt = pa != null ? pa.getInput() : normalP.getInput();
            if (!pm.id().equals(io.ticticboom.mods.mm.util.PortUtils.id(expectedPath, pm.input()))) {
                return false;
            }
            if (expectedInputOpt.isPresent() && !expectedInputOpt.get().equals(pm.input())) return false;
            portMatch = true;
        } else if (requiredPiece instanceof PortTypeAnywhereStructurePiece || requiredPiece instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece) {
            PortTypeAnywhereStructurePiece pta = null;
            io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece normalPT = null;
            if (requiredPiece instanceof PortTypeAnywhereStructurePiece) pta = (PortTypeAnywhereStructurePiece) requiredPiece;
            if (requiredPiece instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece) normalPT = (io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece) requiredPiece;
            ResourceLocation expectedType = pta != null ? pta.getPortTypeId() : normalPT.getPortTypeId();
            Optional<Boolean> expectedInputOpt = pta != null ? pta.getInput() : normalPT.getInput();
            int minTier = pta != null ? pta.getMinTier() : normalPT.getMinTier();
            int maxTier = pta != null ? pta.getMaxTier() : normalPT.getMaxTier();
            if (!pbe.getModel().type().equals(expectedType)) return false;
            if (expectedInputOpt.isPresent() && !expectedInputOpt.get().equals(pbe.getModel().input())) return false;
            var storageModel = pbe.getModel().config().getModel();
            int candidateRank = storageModel.getTierRank();
            try {
                if (candidateRank <= 0 && pbe.getModel().jsonConfig() != null && pbe.getModel().jsonConfig().has("tierRank")) {
                    candidateRank = pbe.getModel().jsonConfig().get("tierRank").getAsInt();
                }
            } catch (Exception ignored) {}
            if (candidateRank <= 0) candidateRank = 1;
            if (candidateRank < minTier) return false;
            portMatch = candidateRank <= maxTier;
        }

        if (!portMatch) return false;

        // If the port matches by type/tier/io, additionally ensure all modifiers on the layout piece are satisfied at this position
        var modifiers = layoutReq.modifiers();
        if (modifiers != null) {
            for (var mod : modifiers) {
                if (!mod.formed(level, absPos, model, rot)) return false;
            }
        }
        return true;
    }

    public RecipeStorages getRecipeStorages(Level level, BlockPos worldControllerPos, StructureModel model) {
        for (var entry : rotatedPositionedPieces.entrySet()) {
            if (innerFormed(level, worldControllerPos, model, entry.getValue(), entry.getKey())) {
                return innerGetRecipeStorages(level, worldControllerPos, entry.getValue());
            }
        }
        return null;
    }

    /** The admin rule for this structure on the level's side (Structure Builder admin tab), or null for its own settings. */
    @Nullable
    static HelperSlotOverrides.Rule helperRule(Level level, @Nullable StructureModel model) {
        return model == null ? null : HelperSlotOverrides.rule(model.id(), level.isClientSide());
    }

    /**
     * Whether an Input Gateway or a Pattern Port may stand in for this piece of the structure: the admin rule's keys
     * when there is one, else the key's own {@code "allowHelper": true}. Never for a port.
     */
    public static boolean helperAllowed(Level level, @Nullable StructureModel model, PositionedLayoutPiece piece) {
        return helperAllowed(piece, helperRule(level, model));
    }

    private static boolean helperAllowed(PositionedLayoutPiece piece, @Nullable HelperSlotOverrides.Rule rule) {
        if (isPortPiece(piece.piece().piece())) {
            return false;
        }
        return rule != null ? rule.keys().contains(piece.piece().valueId().charAt(0)) : piece.piece().allowsHelper();
    }

    /**
     * Whether ports of this layout may move or be left out: a key takes a port in place of its block
     * ({@code "allowPort": true}) or a port key names a {@code "replaceWith"}.
     */
    public boolean hasPortSlots() {
        return portSlots != null;
    }

    /** The keys whose blocks a port may replace, which are also the blocks a position takes once its port has moved. */
    public List<StructureLayoutPiece> portFillers() {
        return portSlots == null ? List.of() : portSlots.fillers();
    }

    /**
     * What may stand where a port of this key is drawn but left out: the key its {@code "replaceWith"} names, else the
     * port slot keys. Empty when the layout has no port slots.
     */
    public List<StructureLayoutPiece> portReplacements(StructureLayoutPiece key) {
        return portSlots == null ? List.of() : portSlots.replacementsFor(key);
    }

    /** The key a port key's {@code "replaceWith"} names, or null. */
    @Nullable
    public StructureLayoutPiece portReplacement(StructureLayoutPiece key) {
        return portSlots == null ? null : portSlots.replacement(key);
    }

    /** How many ports of this port key the structure takes, or null when the layout has no port slots or it is no port key. */
    @Nullable
    public PortLimit portLimit(StructureLayoutPiece key) {
        return portSlots == null ? null : portSlots.limit(key);
    }

    public static boolean isPortPiece(Object underlying) {
        return underlying instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece
                || underlying instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece
                || underlying instanceof PortAnywhereStructurePiece
                || underlying instanceof PortTypeAnywhereStructurePiece;
    }

    /** An Input Gateway or a Pattern Port stands in for a plain structure block only where {@link #helperAllowed} says so. */
    static boolean isHelperInPlace(Level level, BlockPos worldControllerPos, PositionedLayoutPiece piece,
                                           @Nullable HelperSlotOverrides.Rule rule) {
        if (!helperAllowed(piece, rule)) {
            return false;
        }
        var state = level.getBlockState(piece.findAbsolutePos(worldControllerPos));
        return state.is(MMRegisters.INPUT_GATEWAY.get()) || state.is(MMRegisters.PATTERN_PORT.get());
    }

    /** Whether the {@code count}-th helper standing in for a piece is still within the admin rule's or the structure's limit. */
    boolean helperFits(@Nullable HelperSlotOverrides.Rule rule, int count) {
        int max = rule != null ? rule.maxHelpers() : maxHelpers;
        return max < 0 || count <= max;
    }

    /**
     * @return every position of the formed structure, or an empty list when not formed
     */
    public List<BlockPos> getPositions(Level level, BlockPos worldControllerPos, StructureModel model) {
        for (var entry : rotatedPositionedPieces.entrySet()) {
            if (innerFormed(level, worldControllerPos, model, entry.getValue(), entry.getKey())) {
                var positions = new ArrayList<BlockPos>();
                for (PositionedLayoutPiece piece : entry.getValue()) {
                    positions.add(piece.findAbsolutePos(worldControllerPos));
                }
                return positions;
            }
        }
        return List.of();
    }

    /**
     * @return positions of the ports of the formed structure, inputs first, or an empty list when not formed
     */
    public List<BlockPos> getPortPositions(Level level, BlockPos worldControllerPos, StructureModel model) {
        for (var entry : rotatedPositionedPieces.entrySet()) {
            if (!innerFormed(level, worldControllerPos, model, entry.getValue(), entry.getKey())) {
                continue;
            }
            var inputs = new ArrayList<BlockPos>();
            var outputs = new ArrayList<BlockPos>();
            for (PositionedLayoutPiece positionedPiece : entry.getValue()) {
                BlockPos absolutePos = positionedPiece.findAbsolutePos(worldControllerPos);
                if (level.getExistingBlockEntity(absolutePos) instanceof IPortBlockEntity pbe) {
                    (pbe.isInput() ? inputs : outputs).add(absolutePos);
                }
            }
            inputs.addAll(outputs);
            return inputs;
        }
        return List.of();
    }

    private RecipeStorages innerGetRecipeStorages(Level level, BlockPos worldControllerPos, List<PositionedLayoutPiece> positionedPieces) {
        var inputStorages = new ArrayList<IPortStorage>();
        var outputStorages = new ArrayList<IPortStorage>();
        for (PositionedLayoutPiece positionedPiece : positionedPieces) {
            BlockPos absolutePos = positionedPiece.findAbsolutePos(worldControllerPos);
            //this works faster
            var be = level.getExistingBlockEntity(absolutePos);
            if(be == null) {
                //just in case of chunk unload
                be = WorldUtil.getBlockEntity(absolutePos, (ServerLevel) level);
            }
            if (be instanceof IPortBlockEntity pbe) {
                if (pbe.isInput()) {
                    inputStorages.add(pbe.getStorage());
                } else {
                    outputStorages.add(pbe.getStorage());
                }
            }
        }
        return new RecipeStorages(inputStorages, outputStorages);
    }

    public static StructureLayout parse(JsonObject json, ResourceLocation structureId) {
        var raw = getCharGrid(json);
        var pieces = getPieces(json, structureId);
        boolean portsAnywhere = false;
        try {
            if (json.has("portsAnywhere")) {
                portsAnywhere = json.get("portsAnywhere").getAsBoolean();
            }
        } catch (Exception ignored) {}
        int maxHelpers = json.has("maxHelpers") ? json.get("maxHelpers").getAsInt() : -1;
        return new StructureLayout(raw, pieces, portsAnywhere, maxHelpers);
    }

    public JsonObject serialize(JsonObject json) {
        json.add("layout", charGrid.serialize());
        var key = new JsonObject();
        for (Map.Entry<StructureKeyChar, StructureLayoutPiece> entry : pieces.entrySet()) {
            key.add("" + entry.getKey().character(), entry.getValue().json());
        }
        json.add("key", key);
        if (maxHelpers >= 0) {
            json.addProperty("maxHelpers", maxHelpers);
        }
        return json;
    }

    public void setup(StructureModel model) {
        for (StructureLayoutPiece value : pieces.values()) {
            value.setup(model);
        }
    }

    private static Map<StructureKeyChar, StructureLayoutPiece> getPieces(JsonObject json, ResourceLocation structureId) {
        Map<StructureKeyChar, StructureLayoutPiece> pieces = new HashMap<>();
        for (var key : json.getAsJsonObject("key").asMap().entrySet()) {
            JsonObject jsonKey = key.getValue().getAsJsonObject();
            pieces.put(new StructureKeyChar(key.getKey().charAt(0)), StructureLayoutPiece.parse(jsonKey, structureId, key.getKey()));
        }
        return pieces;
    }

    private static StructureCharacterGrid getCharGrid(JsonObject json) {
        ArrayList<List<String>> rawLayout = new ArrayList<>();
        JsonArray layers = json.get("layout").getAsJsonArray();
        for (var layer : layers) {
            var resultRows = new ArrayList<String>();
            JsonArray rows = layer.getAsJsonArray();
            for (var row : rows) {
                resultRows.add(row.getAsString());
            }
            rawLayout.add(resultRows);
        }
        return new StructureCharacterGrid(rawLayout);
    }
}
