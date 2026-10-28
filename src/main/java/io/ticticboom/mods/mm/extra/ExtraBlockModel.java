package io.ticticboom.mods.mm.extra;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record ExtraBlockModel(
        String id,
        String name,
        Component displayName,
        Identifier type,
        String connectGroup
) {
    public static ExtraBlockModel parse(JsonObject json) {
        String id = json.get("id").getAsString();
        // The raw string seeds the generated block.mm.<id> lang entry; the component form is what
        // the block actually displays when the pack supplied a translation key.
        String name = ParserUtils.parseComponentKey(json.get("name"));
        Component displayName = ParserUtils.parseComponent(json.get("name"));
        Identifier type = ParserUtils.parseId(json, "type");
        String connectGroup = json.has("connectGroup") ? json.get("connectGroup").getAsString() : null;
        return new ExtraBlockModel(id, name, displayName, type, connectGroup);
    }

    public String resolvedConnectGroup() {
        return connectGroup != null ? connectGroup : id;
    }
}
