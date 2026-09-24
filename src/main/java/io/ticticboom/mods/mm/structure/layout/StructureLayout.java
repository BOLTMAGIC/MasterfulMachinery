package io.ticticboom.mods.mm.structure.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.port.PortAnywhereStructurePiece;
import io.ticticboom.mods.mm.piece.type.porttype.PortTypeAnywhereStructurePiece;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WorldUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

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

    public StructureLayout(StructureCharacterGrid rawLayout, Map<StructureKeyChar, StructureLayoutPiece> pieces, boolean portsAnywhereGlobal) {
        this.charGrid = rawLayout;
        this.pieces = pieces;
        this.portsAnywhereGlobal = portsAnywhereGlobal;
        setupVariants();
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
        // First pass: validate all non-anywhere pieces in-place
        List<PositionedLayoutPiece> anywherePieces = new ArrayList<>();
        for (PositionedLayoutPiece piece : positionedPieces) {
            var layoutPiece = piece.piece();
            var underlying = layoutPiece.piece();
            // treat port pieces as anywhere if either they are explicitly anywhere OR the layout enables portsAnywhereGlobal
            if (underlying instanceof PortAnywhereStructurePiece || underlying instanceof PortTypeAnywhereStructurePiece
                    || (portsAnywhereGlobal && (underlying instanceof io.ticticboom.mods.mm.piece.type.port.PortStructurePiece || underlying instanceof io.ticticboom.mods.mm.piece.type.porttype.PortTypeStructurePiece))) {
                anywherePieces.add(piece);
                continue; // skip per-position check for anywhere pieces
            }
            if (!piece.formed(level, worldControllerPos, model, rot)) {
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

    private boolean candidateMatches(StructureLayoutPiece layoutReq, Object requiredPiece, Level level, BlockPos absPos, Rotation rot, StructureModel model) {
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
        return new StructureLayout(raw, pieces, portsAnywhere);
    }

    public JsonObject serialize(JsonObject json) {
        json.add("layout", charGrid.serialize());
        var key = new JsonObject();
        for (Map.Entry<StructureKeyChar, StructureLayoutPiece> entry : pieces.entrySet()) {
            key.add("" + entry.getKey().character(), entry.getValue().json());
        }
        json.add("key", key);
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
