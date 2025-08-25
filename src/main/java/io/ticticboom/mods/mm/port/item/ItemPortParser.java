package io.ticticboom.mods.mm.port.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.ParserUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class ItemPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        int rows = json.get("rows").getAsInt();
        int columns = json.get("columns").getAsInt();
        Supplier<Boolean> autoPushSupplier = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        return new ItemPortStorageFactory(new ItemPortStorageModel(rows, columns, autoPushSupplier));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var count = json.get("count").getAsInt();
        if (json.has("item")) {
            var itemId = ParserUtils.parseId(json, "item");
            return new SingleItemPortIngredient(itemId, count);
        } else if (json.has("tag")) {
            var tagId = ParserUtils.parseId(json, "tag");
            return new TagItemPortIngredient(tagId, count);
        }
        throw new RuntimeException("Invalid recipe item ingredient, neither item, not tag was found.");
    }
}
