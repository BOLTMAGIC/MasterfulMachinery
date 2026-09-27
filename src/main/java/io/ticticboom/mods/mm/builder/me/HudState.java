package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the Multiblock Tool's HUD shows for a player's crafts ({@link CraftTracker#hud}), sent to the client as
 * {@code ToolHudPkt}.
 *
 * @param done       items already crafted, by count
 * @param total      items requested, failed crafts left out
 * @param inProgress up to {@value #ICONS} items still calculating or crafting
 * @param failure    the newest failed craft ("Oak Planks ×12: missing ingredients in ME"), or null
 */
public record HudState(Phase phase, int done, int total, List<Item> inProgress, @Nullable Component failure) {
    public static final int ICONS = 3;
    public static final HudState NONE = new HudState(Phase.NONE, 0, 0, List.of(), null);

    public enum Phase {
        /** No tracked craft: the HUD is hidden. */
        NONE,
        /** Crafts on their way: the progress bar. */
        CRAFTING,
        /** Crafts on their way, but the bound network can't be reached or has no power. */
        WAITING,
        /** Everything crafted: right-click to build. */
        READY,
        /** Nothing left on its way, and some crafts failed. */
        FAILED
    }
}
