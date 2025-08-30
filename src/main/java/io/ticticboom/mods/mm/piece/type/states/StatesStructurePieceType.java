package io.ticticboom.mods.mm.piece.type.states;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.type.MMStructurePieceType;
import io.ticticboom.mods.mm.piece.type.StructurePiece;

public class StatesStructurePieceType extends MMStructurePieceType {
    @Override
    public boolean identify(JsonObject json) {
        return json.has("stateList");
    }

    @Override
    public StructurePiece parse(JsonObject json) {
        var listName = json.get("stateList").getAsString();
        var translationKey = "mm.structure.piece.states.display_key";
        if (json.has("nameTranslationKey")) {
            translationKey = json.get("nameTranslationKey").getAsString();
        }
        return new StatesStructurePiece(listName, translationKey);
    }
}
