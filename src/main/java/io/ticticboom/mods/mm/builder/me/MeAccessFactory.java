package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Produces a {@link MeAccess} for a held Multiblock Tool, or null when AE2 isn't installed, the tool
 * isn't bound, or its network can't be used right now (unreachable, or the player has no access).
 * <p>
 * The lookups are registered by {@code compat.ae2} during mod setup, only when AE2 is loaded
 * (the same pattern as {@code NetworkLink#LINKER}) so common code never imports {@code appeng.*}.
 */
public final class MeAccessFactory {
    @Nullable
    private static BiFunction<ServerPlayer, ItemStack, MeAccess> lookup;
    @Nullable
    private static BiFunction<ServerPlayer, ItemStack, Component> problem;

    private MeAccessFactory() {
    }

    /**
     * Called once from AE2 compat init; never touched when AE2 is absent.
     *
     * @param access  the tool's usable network, or null
     * @param problem why a bound tool with ME turned on gets no network (for the player), or null
     */
    public static void setLookup(BiFunction<ServerPlayer, ItemStack, MeAccess> access, BiFunction<ServerPlayer, ItemStack, Component> problem) {
        MeAccessFactory.lookup = access;
        MeAccessFactory.problem = problem;
    }

    @Nullable
    public static MeAccess forTool(ServerPlayer player, ItemStack tool) {
        return lookup == null ? null : lookup.apply(player, tool);
    }

    /**
     * Why {@link #forTool} gives null although the tool is bound and ME is turned on ("unreachable", "no access"), or
     * null when there is nothing to tell (AE2 absent, not bound, ME off, or the network is fine).
     */
    @Nullable
    public static Component problem(ServerPlayer player, ItemStack tool) {
        return problem == null ? null : problem.apply(player, tool);
    }
}
