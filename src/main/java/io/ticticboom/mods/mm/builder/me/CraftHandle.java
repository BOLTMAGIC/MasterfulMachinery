package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks one craft requested through {@link MeAccess#requestCraft}. {@code CraftTracker} (a later
 * task) polls these to drive the HUD.
 */
public interface CraftHandle {
    enum State {
        /** The crafting plan is still being calculated. */
        CALCULATING,
        /** A plan was found and submitted; the crafting CPU is working. */
        CRAFTING,
        /** The requested amount is now in the network. */
        DONE,
        /** The plan couldn't be completed (missing ingredients, cancelled, ...). */
        FAILED
    }

    State state();

    Item item();

    int amount();

    /** Reason for {@link State#FAILED}, else null. */
    @Nullable
    Component failure();
}
