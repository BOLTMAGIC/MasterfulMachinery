package io.ticticboom.mods.mm.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class StructureCaptureUtil {

    private static final char[] KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    public static class CaptureResult {
        public boolean success;
        public String error;
        public String baseName;
        public String jsonPath;
        public String jsPath;
        public String nbtPath; // will be null when NBT generation disabled
    }

    public static CaptureResult captureAndSave(Level level, BlockPos p1, BlockPos p2, String playerName) {
        CaptureResult r = new CaptureResult();
        if (!(level instanceof ServerLevel serverLevel)) {
            r.success = false;
            r.error = "Must be run on server";
            return r;
        }

        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;

        long maxBlocks = 50000L; // default limit
        long count = 0L;

        Map<String, Character> keyMap = new LinkedHashMap<>();
        Map<Character, JsonObject> keyDefs = new LinkedHashMap<>();
        Set<Character> usedChars = new HashSet<>();

        ResourceLocation foundController = null;
        BlockPos controllerPos = null;

        // grid to hold characters for rotation; index: [relY][relZ][relX]
        char[][][] grid = new char[dy][dz][dx];
        for (int yy = 0; yy < dy; yy++) for (int zz = 0; zz < dz; zz++) for (int xx = 0; xx < dx; xx++) grid[yy][zz][xx] = ' ';

        // scan region
        for (int y = maxY; y >= minY; y--) {
            for (int z = maxZ; z >= minZ; z--) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!serverLevel.isLoaded(pos)) {
                        r.success = false;
                        r.error = "Chunk not loaded at " + pos;
                        return r;
                    }
                    var state = serverLevel.getBlockState(pos);
                    Block b = state.getBlock();
                    String key = "minecraft:air";
                    if (!b.equals(net.minecraft.world.level.block.Blocks.AIR)) {
                        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(b);
                        if (rl != null) key = rl.toString(); else key = b.toString();
                        // Always ignore podzol (treat as air)
                        if ("minecraft:podzol".equals(key)) {
                            key = "minecraft:air";
                        } else {
                            if (b instanceof IControllerPart part) {
                                // Prefer the lowest controller in Y
                                if (foundController == null || pos.getY() < controllerPos.getY()) {
                                    foundController = Ref.id(part.getModel().id());
                                    controllerPos = pos;
                                }
                            }
                        }
                    }

                    int relX = x - minX;
                    int relY = y - minY; // bottom=0
                    int relZ = z - minZ;

                    if ("minecraft:air".equals(key)) {
                        grid[relY][relZ][relX] = ' ';
                    } else {
                        count++;
                        if (count > maxBlocks) {
                            r.success = false;
                            r.error = "Max blocks exceeded: " + count + " > " + maxBlocks;
                            return r;
                        }
                        Character chr = keyMap.get(key);
                        if (chr == null) {
                            // limit check: available characters minus reserved 'C'
                            if (keyMap.size() >= KEYS.length - 1) {
                                r.success = false;
                                r.error = "Too many unique block types (>" + (KEYS.length - 1) + ")";
                                return r;
                            }
                            boolean isControllerBlock = serverLevel.getBlockState(pos).getBlock() instanceof IControllerPart;
                            if (isControllerBlock) {
                                chr = 'C';
                                if (usedChars.contains(chr)) {
                                    // remap existing owner of 'C'
                                    String otherKey = null;
                                    for (var e2 : keyMap.entrySet()) {
                                        if (e2.getValue().charValue() == chr.charValue()) {
                                            otherKey = e2.getKey();
                                            break;
                                        }
                                    }
                                    if (otherKey != null) {
                                        char newChar = pickNextKey(usedChars);
                                        keyMap.put(otherKey, newChar);
                                        JsonObject moved = keyDefs.remove(chr);
                                        if (moved != null) {
                                            keyDefs.put(newChar, moved);
                                        } else {
                                            JsonObject defMoved = new JsonObject();
                                            defMoved.addProperty("block", otherKey);
                                            keyDefs.put(newChar, defMoved);
                                        }
                                        usedChars.remove(chr);
                                        usedChars.add(newChar);
                                    }
                                }
                            } else {
                                chr = pickNextKey(usedChars);
                            }
                            keyMap.put(key, chr);
                            if (chr != 'C') {
                                JsonObject def = new JsonObject();
                                def.addProperty("block", key);
                                keyDefs.put(chr, def);
                            }
                            usedChars.add(chr);
                        }
                        grid[relY][relZ][relX] = chr;
                    }
                }
            }
        }

        // determine rotation so that controller is at front (z == newDz-1)
        int rotation = 0; // degrees clockwise
        @SuppressWarnings("unused") int newDxLocal = dx, newDzLocal = dz;
        if (foundController != null && controllerPos != null) {
            int ctrlRelX = controllerPos.getX() - minX;
            int ctrlRelZ = controllerPos.getZ() - minZ;
            int chosen = 0; // 0,90,180,270
            for (int cand : new int[]{0,90,180,270}) {
                int nz, ndz;
                if (cand == 0) { nz = ctrlRelZ; ndz = dz; }
                else if (cand == 90) { nz = dx - 1 - ctrlRelX; ndz = dx; }
                else if (cand == 180) { nz = dz - 1 - ctrlRelZ; ndz = dz; }
                else { nz = ctrlRelX; ndz = dx; }
                if (nz == ndz - 1) { chosen = cand; break; }
            }
            rotation = chosen;
        }

        // apply rotation to grid
        char[][][] rotatedGrid = grid;
        int finalDx = dx, finalDz = dz;
        if (rotation != 0) {
            if (rotation == 90) {
                finalDx = dz; finalDz = dx;
                char[][][] tmp = new char[dy][finalDz][finalDx];
                for (int y = 0; y < dy; y++) for (int z = 0; z < finalDz; z++) for (int x = 0; x < finalDx; x++) tmp[y][z][x] = ' ';
                for (int y = 0; y < dy; y++) {
                    for (int z = 0; z < dz; z++) {
                        for (int x = 0; x < dx; x++) {
                            char v = grid[y][z][x];
                            int nz = dx - 1 - x;
                            tmp[y][nz][z] = v;
                        }
                    }
                }
                rotatedGrid = tmp;
            } else if (rotation == 180) {
                char[][][] tmp = new char[dy][dz][dx];
                for (int y = 0; y < dy; y++) for (int z = 0; z < dz; z++) for (int x = 0; x < dx; x++) tmp[y][z][x] = ' ';
                for (int y = 0; y < dy; y++) {
                    for (int z = 0; z < dz; z++) {
                        for (int x = 0; x < dx; x++) {
                            char v = grid[y][z][x];
                            int nx = dx - 1 - x;
                            int nz = dz - 1 - z;
                            tmp[y][nz][nx] = v;
                        }
                    }
                }
                rotatedGrid = tmp;
            } else {
                finalDx = dz; finalDz = dx;
                char[][][] tmp = new char[dy][finalDz][finalDx];
                for (int y = 0; y < dy; y++) for (int z = 0; z < finalDz; z++) for (int x = 0; x < finalDx; x++) tmp[y][z][x] = ' ';
                for (int y = 0; y < dy; y++) {
                    for (int z = 0; z < dz; z++) {
                        for (int x = 0; x < dx; x++) {
                            char v = grid[y][z][x];
                            int nx = dz - 1 - z;
                            tmp[y][x][nx] = v;
                        }
                    }
                }
                rotatedGrid = tmp;
            }
        }

        // build JSON layout object from rotatedGrid
        JsonObject root = new JsonObject();
        root.addProperty("name", "captured_structure");
        JsonObject layout = new JsonObject();
        JsonArray layersArray = new JsonArray();
        for (int y = dy - 1; y >= 0; y--) {
            JsonArray layerArr = new JsonArray();
            for (int z = 0; z < finalDz; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < finalDx; x++) {
                    row.append(rotatedGrid[y][z][x]);
                }
                layerArr.add(row.toString());
            }
            layersArray.add(layerArr);
        }
        layout.add("layers", layersArray);
        JsonObject keyJson = new JsonObject();
        for (var e : keyDefs.entrySet()) {
            keyJson.add(String.valueOf(e.getKey()), e.getValue());
        }
        layout.add("key", keyJson);
        root.add("layout", layout);

        // if a controller was found, add controllerId and rotated offset
        if (foundController != null && controllerPos != null) {
            int offX = controllerPos.getX() - minX;
            int offY = controllerPos.getY() - minY;
            int offZ = controllerPos.getZ() - minZ;
            int rx = offX, rz = offZ;
            if (rotation == 90) { rx = offZ; rz = dx - 1 - offX; }
            else if (rotation == 180) { rx = dx - 1 - offX; rz = dz - 1 - offZ; }
            else if (rotation == 270) { rx = dz - 1 - offZ; rz = offX; }
            JsonArray off = new JsonArray();
            off.add(rx); off.add(offY); off.add(rz);
            root.addProperty("controllerId", foundController.toString());
            root.add("controllerOffset", off);
        }

        // create kubejs script
        String baseName = getNextBaseName(playerName);

        String jsonString = new Gson().toJson(root);
        String js = buildKubeJs(baseName, root, keyMap);

        try {
            Path outDir = Paths.get("config", "mm", "structures");
            Files.createDirectories(outDir);
            Path jsonPath = outDir.resolve(baseName + ".json");
            Path jsPath = outDir.resolve(baseName + ".js");

            Files.writeString(jsonPath, jsonString, StandardCharsets.UTF_8);
            Files.writeString(jsPath, js, StandardCharsets.UTF_8);

            r.success = true;
            r.baseName = baseName;
            r.jsonPath = jsonPath.toString();
            r.jsPath = jsPath.toString();
            r.nbtPath = null; // intentionally not creating NBT files anymore
            return r;
        } catch (IOException e) {
            r.success = false;
            r.error = "IO error: " + e.getMessage();
            return r;
        }
    }

    private static String getNextBaseName(String playerName) {
        try {
            Path outDir = Paths.get("config", "mm", "structures");
            if (!Files.exists(outDir)) return safeBase(playerName, 1);
            String prefix = "mm_capture_" + sanitize(playerName) + "_multiblock_";
            int max = 0;
            try (var s = Files.list(outDir)) {
                Iterator<Path> it = s.iterator();
                while (it.hasNext()) {
                    Path p = it.next();
                    String name = p.getFileName().toString();
                    if (name.startsWith(prefix)) {
                        String rest = name.substring(prefix.length());
                        int idx = rest.indexOf('.');
                        if (idx > 0) rest = rest.substring(0, idx);
                        try {
                            int v = Integer.parseInt(rest);
                            if (v > max) max = v;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            return safeBase(playerName, max + 1);
        } catch (IOException e) {
            return safeBase(playerName, 1);
        }
    }

    private static String safeBase(String playerName, int n) {
        return "mm_capture_" + sanitize(playerName) + "_multiblock_" + n;
    }

    private static String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }

    private static char pickNextKey(Set<Character> used) {
        for (char c : KEYS) {
            if (c == 'C') continue; // reserve C for controller
            if (!used.contains(c)) return c;
        }
        throw new IllegalStateException("No available key characters");
    }

    private static String buildKubeJs(String id, JsonObject root, Map<String, Character> keyMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("priority: 1;\n");
        sb.append("MMEvents.createStructures((event) => {\n");
        sb.append("  event\n");
        // prefer controller name (if present) for create id and display name
        String createIdFinal = id;
        if (root.has("controllerId") && !root.get("controllerId").getAsString().isBlank()) {
            String controllerIdStr = root.get("controllerId").getAsString();
            String base = controllerIdStr.contains(":" ) ? controllerIdStr.substring(controllerIdStr.indexOf(":") + 1) : controllerIdStr;
            if (base.endsWith("_controller")) base = base.substring(0, base.length() - "_controller".length());
            createIdFinal = base + "_structure";
            sb.append("    .create('mm:").append(createIdFinal).append("')\n");
            sb.append("    .controllerId('").append(controllerIdStr).append("')\n");
            String displayName = getString(base);
            sb.append("    .name('").append(displayName).append("')\n");
        } else {
            if (!createIdFinal.endsWith("_structure")) createIdFinal = createIdFinal + "_structure";
            sb.append("    .create('mm:").append(createIdFinal).append("')\n");
            if (root.has("controllerId")) {
                sb.append("    .controllerId('").append(root.get("controllerId").getAsString()).append("')\n");
            } else {
                sb.append("    .controllerId('')\n");
            }
            String rawName = root.has("name") ? root.get("name").getAsString() : "";
            if (rawName == null || rawName.isBlank()) {
                String base = createIdFinal;
                if (base.endsWith("_structure")) base = base.substring(0, base.length() - "_structure".length());
                rawName = base;
            }
            String displayName = getString(rawName);
            sb.append("    .name('").append(displayName).append("')\n");
        }
        sb.append("    .layout((a) => {\n");
        var layers = root.getAsJsonObject("layout").getAsJsonArray("layers");
        for (int i = 0; i < layers.size(); i++) {
            sb.append("      a.layer([");
            var arr = layers.get(i).getAsJsonArray();
            sb.append("\n");
            for (int r = 0; r < arr.size(); r++) {
                sb.append("        '").append(arr.get(r).getAsString().replace("'", "\\'"))
                        .append(r == arr.size() - 1 ? "'\n" : "',\n");
            }
            sb.append("      ]")
                    .append(" )\n");
        }
        sb.append("      ");
        // keys (skip controller 'C' — it must not be present under key definitions)
        for (var e : keyMap.entrySet()) {
            String block = e.getKey();
            char chr = e.getValue();
            if (chr == 'C') continue;
            sb.append("        .key('").append(chr).append("', {\n");
            sb.append("          block: '").append(block).append("',\n");
            sb.append("        })\n");
        }
        sb.append("    });\n");
        sb.append("});\n");
        return sb.toString();
    }

    private static @NotNull String getString(String rawName) {
        String[] partsName = rawName.replace('_', ' ').split("\\s+");
        StringBuilder nameBuilder = new StringBuilder();
        for (String p : partsName) {
            if (p.isEmpty()) continue;
            String part = p.substring(0, 1).toUpperCase(Locale.ROOT) + (p.length() > 1 ? p.substring(1).toLowerCase(Locale.ROOT) : "");
            if (!nameBuilder.isEmpty()) nameBuilder.append(' ');
            nameBuilder.append(part);
        }
        return nameBuilder.toString();
    }

}







