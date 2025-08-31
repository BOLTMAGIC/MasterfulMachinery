package io.ticticboom.mods.mm.structure.attachment.states;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.MMStructurePieceRegistry;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record NamedStateList(String name, boolean defaultIgnored, Map<String, StructurePiece> pieces) {
    public static final String DEFAULT_STATE = "default";
    private static final List<String> specialKeys = List.of("defaultIgnored");

    public StateListPieceFormedResult formed(Level level, BlockPos pos, StructureModel model) {
        for (Map.Entry<String, StructurePiece> entry : pieces.entrySet()) {
            var piece = entry.getValue();
            boolean formed = piece.formed(level, pos, model);
            if (formed) {
                return new StateListPieceFormedResult(true, entry.getKey());
            }
        }
        if (defaultIgnored)
        {
            return new StateListPieceFormedResult(true, DEFAULT_STATE);
        }

        return new StateListPieceFormedResult(false, null);
    }

    public static NamedStateList parse(String name, JsonObject json) {
        var pieces = json.entrySet().stream()
                .filter(entry -> !specialKeys.contains(entry.getKey()))
                .map(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        throw new IllegalArgumentException(String.format("State [%s] inside of list [%s] must be an object", entry.getKey(), name));
                    }
                    var obj = entry.getValue().getAsJsonObject();
                    var piece = MMStructurePieceRegistry.findPieceType(obj);
                    if (piece == null) {
                        throw new IllegalArgumentException(String.format("State [%s] inside of list [%s] must be a valid piece type. Problem JSON: %s", entry.getKey(), name, obj.toString()));
                    }
                    return Map.entry(entry.getKey(), piece);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var defaultIgnored = false;
        if (json.has("defaultIgnored")) {
            defaultIgnored = json.get("defaultIgnored").getAsBoolean();
        }
        return new NamedStateList(name, defaultIgnored, pieces);
    }
}
