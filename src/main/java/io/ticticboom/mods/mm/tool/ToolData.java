package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * NBT accessors for the Multiblock Tool's own settings: the selected structure, the extra rotation
 * the player dialed in with Shift+scroll, the tool's remembered port tier preferences, instant-build choice,
 * and its ME network binding with the two toggles that control it.
 */
public final class ToolData {
    private static final String STRUCTURE_KEY = "Structure";
    /** Set when the selected structure is another mod's (a BuildableStructure), not an MM one. */
    private static final String BUILDER_KEY = "BuilderStructure";
    private static final String TURNS_KEY = "ExtraTurns";
    private static final String TIERS_KEY = "Tiers";
    private static final String NETWORK_KEY = "Network";
    private static final String USE_ME_KEY = "UseMe";
    private static final String AUTOCRAFT_KEY = "AutoCraft";
    private static final String INSTANT_BUILD_KEY = "InstantBuild";

    private static final String ANCHOR_KEY = "PlacementAnchor";

    public record Anchor(ResourceLocation dimension, BlockPos pos, Direction face, Direction facing) {
        public Anchor {
            pos = pos.immutable();
            if (facing.getAxis().isVertical()) throw new IllegalArgumentException("Anchor facing must be horizontal");
        }
    }

    @Nullable
    public static Anchor anchor(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ANCHOR_KEY, Tag.TAG_COMPOUND)) return null;
        CompoundTag tag = root.getCompound(ANCHOR_KEY);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        Direction face = Direction.byName(tag.getString("Face"));
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (dimension == null || face == null || facing == null || facing.getAxis().isVertical()
                || !tag.contains("Pos", Tag.TAG_LONG)) return null;
        return new Anchor(dimension, BlockPos.of(tag.getLong("Pos")), face, facing);
    }

    public static void setAnchor(ItemStack stack, @Nullable Anchor anchor) {
        if (anchor == null) {
            if (stack.hasTag()) stack.getTag().remove(ANCHOR_KEY);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", anchor.dimension().toString());
        tag.putLong("Pos", anchor.pos().asLong());
        tag.putString("Face", anchor.face().getName());
        tag.putString("Facing", anchor.facing().getName());
        stack.getOrCreateTag().put(ANCHOR_KEY, tag);
    }

    public static Direction placementFacing(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        Anchor anchor = anchor(stack);
        return anchor != null && anchor.dimension().equals(player.level().dimension().location())
                ? anchor.facing() : player.getDirection();
    }

    private ToolData() {
    }

    /** The selected MM structure, or null when none is selected or the selection is another mod's structure. */
    public static @Nullable ResourceLocation structure(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STRUCTURE_KEY, Tag.TAG_STRING) || tag.getBoolean(BUILDER_KEY)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(STRUCTURE_KEY));
    }

    /** The selected non-MM structure (a {@code BuildableStructure} id), or null. */
    public static @Nullable ResourceLocation builderStructure(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STRUCTURE_KEY, Tag.TAG_STRING) || !tag.getBoolean(BUILDER_KEY)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(STRUCTURE_KEY));
    }

    /** Selects an MM structure (null clears the selection). */
    public static void setStructure(ItemStack stack, @Nullable ResourceLocation id) {
        setAnchor(stack, null);
        if (id == null) {
            if (stack.hasTag()) {
                //noinspection DataFlowIssue - hasTag() just confirmed the tag exists
                stack.getTag().remove(STRUCTURE_KEY);
                stack.getTag().remove(BUILDER_KEY);
            }
            return;
        }
        stack.getOrCreateTag().putString(STRUCTURE_KEY, id.toString());
        stack.getOrCreateTag().remove(BUILDER_KEY);
    }

    /** Selects another mod's structure (loaded from a datapack {@code .nbt}). */
    public static void setBuilderStructure(ItemStack stack, ResourceLocation id) {
        setAnchor(stack, null);
        stack.getOrCreateTag().putString(STRUCTURE_KEY, id.toString());
        stack.getOrCreateTag().putBoolean(BUILDER_KEY, true);
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

    /** Whether this tool places its entire prepared build in one server tick. Default false. */
    public static boolean instantBuild(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(INSTANT_BUILD_KEY);
    }

    public static void setInstantBuild(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(INSTANT_BUILD_KEY, value);
    }
}
