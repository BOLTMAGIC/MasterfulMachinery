package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.util.ItemComponentUtil;
import net.minecraft.core.component.DataComponentPatch;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.NbtMatchUtils;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

public class ItemPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        int rows = json.get("rows").getAsInt();
        int columns = json.get("columns").getAsInt();
        Supplier<Boolean> autoPushSupplier = ParserUtils.parseOrDefaultSupplier(json, "autoPush", () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, JsonElement::getAsBoolean);
        int slotCapacity = 0;
        if (json.has("slotCapacity")) {
            try {
                slotCapacity = Math.max(0, Math.min(ItemPortHandler.MAX_SLOT_CAPACITY, json.get("slotCapacity").getAsInt()));
            } catch (Exception ignored) {
            }
        }
        int tierRank = 0;
        if (json.has("tierRank")) {
            try {
                tierRank = json.get("tierRank").getAsInt();
            } catch (Exception ignored) {
            }
        }
        return new ItemPortStorageFactory(new ItemPortStorageModel(rows, columns, autoPushSupplier, slotCapacity, tierRank));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var count = json.has("count") ? AmountRange.parse(json, "count") : AmountRange.of(1);
        CompoundTag requiredNbt = null;
        boolean nbtStrong = false;
        if (json.has("nbt")) {
            try {
                requiredNbt = NbtMatchUtils.parseFromJson(json.get("nbt"));
            } catch (Exception e) {
            }
        } else if (json.has("nbt_snbt")) {
            try {
                requiredNbt = NbtMatchUtils.parseFromJson(json.get("nbt_snbt"));
            } catch (Exception e) {
            }
        }
        DataComponentPatch components = json.has("components") ? ItemComponentUtil.parse(json.get("components")) : DataComponentPatch.EMPTY;
        if (json.has("nbt_match")) {
            try {
                var v = json.get("nbt_match").getAsString();
                nbtStrong = "strong".equalsIgnoreCase(v);
            } catch (Exception e) {
            }
        }

        if (json.has("item")) {
            var itemId = ParserUtils.parseId(json, "item");
            return new SingleItemPortIngredient(itemId, count, requiredNbt, nbtStrong, components);
        } else if (json.has("tag")) {
            var tagId = ParserUtils.parseId(json, "tag");
            return new TagItemPortIngredient(tagId, count, requiredNbt, nbtStrong, components);
        }
        throw new RuntimeException("Invalid recipe item ingredient, neither item, not tag was found.");
    }
}
