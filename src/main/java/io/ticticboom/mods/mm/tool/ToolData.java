package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * NBT accessors for the Multiblock Tool's own settings: the selected structure, the extra rotation
 * the player dialed in with Shift+scroll, the tool's remembered port tier preferences, and its ME
 * network binding with the two toggles that control it.
 */
public final class ToolData {
    private static final String STRUCTURE_KEY = "Structure";
    private static final String TURNS_KEY = "ExtraTurns";
    private static final String TIERS_KEY = "Tiers";
    private static final String NETWORK_KEY = "Network";
    private static final String USE_ME_KEY = "UseMe";
    private static final String AUTOCRAFT_KEY = "AutoCraft";

    private ToolData() {
    }

    public static @Nullable ResourceLocation structure(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STRUCTURE_KEY, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(STRUCTURE_KEY));
    }

    public static void setStructure(ItemStack stack, @Nullable ResourceLocation id) {
        if (id == null) {
            if (stack.hasTag()) {
                //noinspection DataFlowIssue - hasTag() just confirmed the tag exists
                stack.getTag().remove(STRUCTURE_KEY);
            }
            return;
        }
        stack.getOrCreateTag().putString(STRUCTURE_KEY, id.toString());
    }

    /** @return 0-3, the number of extra quarter turns applied on top of the player's facing. */
    public static int extraTurns(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        return Math.floorMod(tag.getInt(TURNS_KEY), 4);
    }

    public static void setExtraTurns(ItemStack stack, int turns) {
        stack.getOrCreateTag().putInt(TURNS_KEY, Math.floorMod(turns, 4));
    }

    public static TierPrefs tiers(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TIERS_KEY, Tag.TAG_COMPOUND)) {
            return new TierPrefs();
        }
        return TierPrefs.load(tag.getCompound(TIERS_KEY));
    }

    public static void setTiers(ItemStack stack, TierPrefs prefs) {
        stack.getOrCreateTag().put(TIERS_KEY, prefs.save());
    }

    /** The ME network the tool is bound to, or null when it isn't bound. */
    @Nullable
    public static LinkData.NetworkPos network(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NETWORK_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return LinkData.NetworkPos.load(tag.getCompound(NETWORK_KEY));
    }

    public static void setNetwork(ItemStack stack, @Nullable LinkData.NetworkPos network) {
        if (network == null) {
            if (stack.hasTag()) {
                //noinspection DataFlowIssue - hasTag() just confirmed the tag exists
                stack.getTag().remove(NETWORK_KEY);
            }
            return;
        }
        stack.getOrCreateTag().put(NETWORK_KEY, network.save());
    }

    /** Whether the tool is allowed to pull from / craft on its ME network. Default true. */
    public static boolean useMe(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(USE_ME_KEY, Tag.TAG_BYTE) || tag.getBoolean(USE_ME_KEY);
    }

    public static void setUseMe(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(USE_ME_KEY, value);
    }

    /** Whether the tool requests auto-crafts for missing materials. Default true. */
    public static boolean autoCraft(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(AUTOCRAFT_KEY, Tag.TAG_BYTE) || tag.getBoolean(AUTOCRAFT_KEY);
    }

    public static void setAutoCraft(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(AUTOCRAFT_KEY, value);
    }
}
