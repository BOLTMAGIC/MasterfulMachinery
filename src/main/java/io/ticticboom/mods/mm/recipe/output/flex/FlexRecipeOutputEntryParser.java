package io.ticticboom.mods.mm.recipe.output.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntryParser;

public class FlexRecipeOutputEntryParser implements IRecipeOutputEntryParser {
    @Override
    public IRecipeOutputEntry parse(JsonObject json) {
        if (!json.has("from") || json.get("from").getAsString().isBlank()) {
            throw new RuntimeException("Missing 'from' (the 'as' name of an mm:input/flex entry) in flex output entry: " + json);
        }
        String from = json.get("from").getAsString();
        String match = json.has("match") ? json.get("match").getAsString() : null;
        String item = json.has("item") ? json.get("item").getAsString() : null;
        var template = FlexTemplate.parse(match, item);
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        boolean copyNbt = json.has("copy_nbt") && json.get("copy_nbt").getAsBoolean();
        double chance = json.has("chance") ? json.get("chance").getAsDouble() : 1.0;
        boolean perTick = json.has("per_tick") && json.get("per_tick").getAsBoolean();
        String scaleBy = json.has("scale_by") ? json.get("scale_by").getAsString() : null;
        return new FlexRecipeOutputEntry(from, template, count, copyNbt, chance, perTick, scaleBy);
    }
}
