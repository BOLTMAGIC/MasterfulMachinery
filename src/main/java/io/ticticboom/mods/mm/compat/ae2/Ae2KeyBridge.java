package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import org.jetbrains.annotations.Nullable;

public interface Ae2KeyBridge {

    String type();

    @Nullable
    GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks);

    @Nullable
    default GenericStack output(IPortIngredient ingredient) {
        return input(ingredient, false, 1);
    }

    boolean handles(AEKey key);

    boolean insert(Ae2PushContext context, AEKey key, long amount);

    default void pushOutputs(RecipeStorages storages, MEStorage me, IActionSource source) {
    }
}
