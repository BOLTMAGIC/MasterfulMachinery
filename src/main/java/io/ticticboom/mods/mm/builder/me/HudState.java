package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record HudState(Phase phase, int done, int total, List<Item> inProgress, @Nullable Component failure) {
    public static final int ICONS = 3;
    public static final HudState NONE = new HudState(Phase.NONE, 0, 0, List.of(), null);

    public enum Phase {
        NONE,
        CRAFTING,
        WAITING,
        READY,
        FAILED
    }
}
