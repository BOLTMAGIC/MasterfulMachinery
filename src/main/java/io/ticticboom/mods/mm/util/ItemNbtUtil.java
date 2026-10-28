package io.ticticboom.mods.mm.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 1.20.5 replaced the single ItemStack NBT tag with typed data components, so getTag,
 * getOrCreateTag, hasTag and setTag are gone. MM's own item data and the nbt matching packs
 * declare on item ingredients both map onto the vanilla minecraft:custom_data component, which is
 * what the game's own data fixer migrates the old tag into. Existing items therefore keep their
 * data across the update.
 * <p>
 * One behaviour change is unavoidable: on 1.20.1 getTag returned everything on the stack, so nbt
 * matching could also match things like Damage or enchantments. Those are now separate components
 * and are not part of custom_data, so a pack whose nbt matched them will no longer match.
 */
public final class ItemNbtUtil {

    private ItemNbtUtil() {
    }

    public static boolean hasTag(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    /** Mirrors the old getTag: null when the stack carries no custom data. */
    public static @Nullable CompoundTag getTag(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.copyTag();
    }

    /** Read-only access that never returns null, for callers that only get values back out. */
    public static CompoundTag getTagOrEmpty(ItemStack stack) {
        var tag = getTag(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        }
    }

    /**
     * Replaces the getOrCreateTag().mutate() pattern. CustomData is immutable, so the component
     * has to be rewritten rather than edited in place.
     */
    public static void mutate(ItemStack stack, Consumer<CompoundTag> mutator) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutator);
    }
}
