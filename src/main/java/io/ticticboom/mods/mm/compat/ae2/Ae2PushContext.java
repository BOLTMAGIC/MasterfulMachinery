package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import org.jetbrains.annotations.Nullable;

public record Ae2PushContext(RecipeModel recipe, RecipeStorages storages, @Nullable MEStorage me, IActionSource source, boolean simulate) {
}
