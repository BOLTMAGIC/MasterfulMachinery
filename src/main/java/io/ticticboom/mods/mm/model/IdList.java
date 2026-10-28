package io.ticticboom.mods.mm.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.util.ParserUtils;
import lombok.Getter;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class IdList {
    @Getter
    private final List<Identifier> ids;

    public IdList(List<Identifier> ids) {

        this.ids = ids;
    }

    public boolean contains(Identifier id) {
        return ids.contains(id);
    }

    public static IdList parse(JsonElement json) {
        List<Identifier> ids = new ArrayList<>();
        if (json.isJsonArray()) {
            JsonArray elems = json.getAsJsonArray();
            for (var elem : elems) {
                ids.add(ParserUtils.parseId(elem));
            }
        } else {
            ids.add(ParserUtils.parseId(json));
        }
        return new IdList(ids);
    }

    public JsonArray serialize() {
        var ids = new JsonArray();
        for (Identifier id : this.ids) {
            ids.add(id.toString());
        }
        return ids;
    }
}
