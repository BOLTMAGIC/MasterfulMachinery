package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ToolData {
    private static final String STRUCTURE_KEY = "Structure";
    private static final String BUILDER_KEY = "BuilderStructure";
    private static final String TURNS_KEY = "ExtraTurns";
    private static final String TIERS_KEY = "Tiers";
    private static final String NETWORK_KEY = "Network";
    private static final String USE_ME_KEY = "UseMe";
    private static final String AUTOCRAFT_KEY = "AutoCraft";
    private static final String INSTANT_BUILD_KEY = "InstantBuild";

    private static final String ANCHOR_KEY = "PlacementAnchor";

    public record Anchor(Identifier dimension, BlockPos pos, Direction face, Direction facing) {
        public Anchor {
            pos = pos.immutable();
            if (facing.getAxis().isVertical()) throw new IllegalArgumentException("Anchor facing must be horizontal");
        }
    }

    @Nullable
    public static Anchor anchor(ItemStack stack) {
        CompoundTag root = tag(stack);
        if (!root.contains(ANCHOR_KEY)) return null;
        CompoundTag tag = root.getCompoundOrEmpty(ANCHOR_KEY);
        Identifier dimension = Identifier.tryParse(tag.getStringOr("Dimension", ""));
        Direction face = Direction.byName(tag.getStringOr("Face", ""));
        Direction facing = Direction.byName(tag.getStringOr("Facing", ""));
        if (dimension == null || face == null || facing == null || facing.getAxis().isVertical()
                || !tag.contains("Pos")) return null;
        return new Anchor(dimension, BlockPos.of(tag.getLongOr("Pos", 0L)), face, facing);
    }

    public static void setAnchor(ItemStack stack, @Nullable Anchor anchor) {
        if (anchor == null) {
            if (tag(stack).contains(ANCHOR_KEY)) ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.remove(ANCHOR_KEY));
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", anchor.dimension().toString());
        tag.putLong("Pos", anchor.pos().asLong());
        tag.putString("Face", anchor.face().getName());
        tag.putString("Facing", anchor.facing().getName());
        ToolComponents.update(stack, ToolComponents.SETTINGS, settings -> settings.put(ANCHOR_KEY, tag));
    }

    public static Direction placementFacing(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        Anchor anchor = anchor(stack);
        return anchor != null && anchor.dimension().equals(player.level().dimension().location())
                ? anchor.facing() : player.getDirection();
    }

    private ToolData() {
    }

    private static CompoundTag tag(ItemStack stack) {
        return ToolComponents.read(stack, ToolComponents.SETTINGS);
    }

    public static @Nullable Identifier structure(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(STRUCTURE_KEY) || tag.getBooleanOr(BUILDER_KEY, false)) {
            return null;
        }
        return Identifier.tryParse(tag.getStringOr(STRUCTURE_KEY, ""));
    }

    public static @Nullable Identifier builderStructure(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(STRUCTURE_KEY) || !tag.getBooleanOr(BUILDER_KEY, false)) {
            return null;
        }
        return Identifier.tryParse(tag.getStringOr(STRUCTURE_KEY, ""));
    }

    public static void setStructure(ItemStack stack, @Nullable Identifier id) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            // a new selection is placed fresh: the old anchor was for the old structure
            tag.remove(ANCHOR_KEY);
            if (id == null) {
                tag.remove(STRUCTURE_KEY);
            } else {
                tag.putString(STRUCTURE_KEY, id.toString());
            }
            tag.remove(BUILDER_KEY);
        });
    }

    public static void setBuilderStructure(ItemStack stack, Identifier id) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            // a new selection is placed fresh: the old anchor was for the old structure
            tag.remove(ANCHOR_KEY);
            tag.putString(STRUCTURE_KEY, id.toString());
            tag.putBoolean(BUILDER_KEY, true);
        });
    }

    public static int extraTurns(ItemStack stack) {
        return Math.floorMod(tag(stack).getIntOr(TURNS_KEY, 0), 4);
    }

    public static void setExtraTurns(ItemStack stack, int turns) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putInt(TURNS_KEY, Math.floorMod(turns, 4)));
    }

    public static TierPrefs tiers(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(TIERS_KEY)) {
            return new TierPrefs();
        }
        return TierPrefs.load(tag.getCompoundOrEmpty(TIERS_KEY));
    }

    public static void setTiers(ItemStack stack, TierPrefs prefs) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.put(TIERS_KEY, prefs.save()));
    }

    @Nullable
    public static LinkData.NetworkPos network(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(NETWORK_KEY)) {
            return null;
        }
        return LinkData.NetworkPos.load(tag.getCompoundOrEmpty(NETWORK_KEY));
    }

    public static void setNetwork(ItemStack stack, @Nullable LinkData.NetworkPos network) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            if (network == null) {
                tag.remove(NETWORK_KEY);
            } else {
                tag.put(NETWORK_KEY, network.save());
            }
        });
    }

    public static boolean useMe(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return !tag.contains(USE_ME_KEY) || tag.getBooleanOr(USE_ME_KEY, false);
    }

    public static void setUseMe(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(USE_ME_KEY, value));
    }

    public static boolean autoCraft(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return !tag.contains(AUTOCRAFT_KEY) || tag.getBooleanOr(AUTOCRAFT_KEY, false);
    }

    public static void setAutoCraft(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(AUTOCRAFT_KEY, value));
    }

    public static boolean instantBuild(ItemStack stack) {
        return tag(stack).getBooleanOr(INSTANT_BUILD_KEY, false);
    }

    public static void setInstantBuild(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(INSTANT_BUILD_KEY, value));
    }
}
