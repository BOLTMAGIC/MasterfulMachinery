package io.ticticboom.mods.mm.builder.me;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public interface CraftHandle {
    enum State {
        CALCULATING,
        CRAFTING,
        DONE,
        FAILED;

        public boolean active() {
            return this == CALCULATING || this == CRAFTING;
        }
    }

    State state();

    Item item();

    int amount();

    @Nullable
    Component failure();

    default void update() {
    }

    default boolean waiting() {
        return false;
    }

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
