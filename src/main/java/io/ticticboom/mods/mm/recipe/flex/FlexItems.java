package io.ticticboom.mods.mm.recipe.flex;

import io.ticticboom.mods.mm.compat.mysticalagriculture.MACrops;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;
import java.util.function.Function;

/**
 * Turns a flex template into the item it names for one input item. Besides ids ("{ns}:$1_essence"), the template
 * can be a token that asks another mod:
 * <ul>
 *     <li>{ma:essence}: the Mystical Agriculture essence of the input seed</li>
 *     <li>{ma:crux}: the crux block the input seed needs (no item if it needs none)</li>
 * </ul>
 */
public final class FlexItems {

    public static final Set<String> TOKENS = Set.of("{ma:essence}", "{ma:crux}");

    private FlexItems() {
    }

    /** Throws if the template looks like a token ({...} on its own) but isn't one MM knows. */
    public static void validate(String item) {
        String text = item.trim();
        if (text.startsWith("{") && text.endsWith("}") && text.indexOf('}') == text.length() - 1
                && !text.equals("{ns}") && !text.equals("{path}") && !TOKENS.contains(text)) {
            throw new RuntimeException("Unknown flex token " + text + ", known: " + TOKENS);
        }
    }

    /**
     * @return the item for this input (count 1, no NBT), or empty if the input doesn't match or the item doesn't exist
     */
    public static ItemStack resolve(FlexTemplate template, ItemStack input) {
        Identifier inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (inputId == null) {
            return ItemStack.EMPTY;
        }
        String text = template.apply(inputId.getNamespace(), inputId.getPath());
        if (text == null) {
            return ItemStack.EMPTY;
        }
        text = text.trim();
        if (text.equals("{ma:essence}")) {
            return fromMA(input, MACrops.Info::essence);
        }
        if (text.equals("{ma:crux}")) {
            return fromMA(input, MACrops.Info::crux);
        }
        Identifier outputId = Identifier.tryParse(text);
        if (outputId == null || !BuiltInRegistries.ITEM.containsKey(outputId)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(outputId));
    }

    /** @param part may return null (e.g. a crop without crux) */
    private static ItemStack fromMA(ItemStack input, Function<MACrops.Info, Item> part) {
        MACrops.Info info = MACrops.forSeeds(input.getItem());
        Item item = info == null ? null : part.apply(info);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
