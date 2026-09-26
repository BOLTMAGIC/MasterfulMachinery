package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks one craft requested through {@link MeAccess#requestCraft}. {@link CraftTracker} advances these on the server
 * thread ({@link #update}) and reads their state for the player.
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
        FAILED;

        /** Still on its way: calculating or crafting. */
        public boolean active() {
            return this == CALCULATING || this == CRAFTING;
        }
    }

    State state();

    Item item();

    int amount();

    /** Reason for {@link State#FAILED}, else null. */
    @Nullable
    Component failure();

    /**
     * Looks at the network once and moves the state on (calculation finished, craft done or cancelled). Called on the
     * server thread; must never wait for anything.
     */
    default void update() {
    }

    /** A request that failed at once (e.g. the network went away meanwhile). */
    static CraftHandle failed(Item item, int amount, Component reason) {
        return new CraftHandle() {
            @Override
            public State state() {
                return State.FAILED;
            }

            @Override
            public Item item() {
                return item;
            }

            @Override
            public int amount() {
                return amount;
            }

            @Override
            public Component failure() {
                return reason;
            }
        };
    }
}
