package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class MeAccessFactory {
    @Nullable
    private static BiFunction<ServerPlayer, ItemStack, MeAccess> lookup;
    @Nullable
    private static BiFunction<ServerPlayer, ItemStack, Component> problem;

    private MeAccessFactory() {
    }

    public static void setLookup(BiFunction<ServerPlayer, ItemStack, MeAccess> access, BiFunction<ServerPlayer, ItemStack, Component> problem) {
        MeAccessFactory.lookup = access;
        MeAccessFactory.problem = problem;
    }

    public static boolean supported() {
        return lookup != null;
    }

    @Nullable
    public static MeAccess forTool(ServerPlayer player, ItemStack tool) {
        return lookup == null ? null : lookup.apply(player, tool);
    }

    @Nullable
    public static Component problem(ServerPlayer player, ItemStack tool) {
        return problem == null ? null : problem.apply(player, tool);
    }
}
