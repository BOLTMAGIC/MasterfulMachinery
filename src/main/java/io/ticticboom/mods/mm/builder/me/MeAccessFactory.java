package io.ticticboom.mods.mm.builder.me;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Produces a {@link MeAccess} for a held Multiblock Tool, or null when AE2 isn't installed, the tool
 * isn't bound, or its network can't be reached right now.
 * <p>
 * The lookup itself is registered by {@code compat.ae2} during mod setup, only when AE2 is loaded
 * (the same pattern as {@code NetworkLink#LINKER}) so common code never imports {@code appeng.*}.
 */
public final class MeAccessFactory {
    @Nullable
    private static BiFunction<ServerPlayer, ItemStack, MeAccess> lookup;

    private MeAccessFactory() {
    }

    /** Called once from AE2 compat init; never touched when AE2 is absent. */
    public static void setLookup(BiFunction<ServerPlayer, ItemStack, MeAccess> value) {
        lookup = value;
    }

    @Nullable
    public static MeAccess forTool(ServerPlayer player, ItemStack tool) {
        return lookup == null ? null : lookup.apply(player, tool);
    }
}
