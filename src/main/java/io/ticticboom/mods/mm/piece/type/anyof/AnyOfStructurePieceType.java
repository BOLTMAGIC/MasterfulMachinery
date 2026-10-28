package io.ticticboom.mods.mm.piece.type.anyof;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.MMStructurePieceRegistry;
import io.ticticboom.mods.mm.piece.type.MMStructurePieceType;
import io.ticticboom.mods.mm.piece.type.StructurePiece;

import java.util.ArrayList;

public class AnyOfStructurePieceType extends MMStructurePieceType {

    @Override
    public boolean identify(JsonObject json) {
        return json.has("anyOf");
    }

    @Override
    public StructurePiece parse(JsonObject json) {
        var options = new ArrayList<StructurePiece>();
        for (JsonElement element : json.getAsJsonArray("anyOf")) {
            if (!element.isJsonObject()) {
                throw new RuntimeException("Every 'anyOf' option must be a structure key object: " + element);
            }
            var option = MMStructurePieceRegistry.findPieceType(element.getAsJsonObject());
            if (option == null) {
                throw new RuntimeException("'anyOf' option is not a valid structure key: " + element);
            }
            options.add(option);
        }
        if (options.isEmpty()) {
            throw new RuntimeException("'anyOf' needs at least one option: " + json);
        }
        return new AnyOfStructurePiece(options);
    }
}
