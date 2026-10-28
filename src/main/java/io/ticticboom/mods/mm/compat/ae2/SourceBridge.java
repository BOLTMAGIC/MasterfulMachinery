package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import gripe._90.arseng.me.key.SourceKey;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortIngredient;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;

final class SourceBridge implements Ae2KeyBridge {

    @Override
    public String type() {
        return "source";
    }

    @Override
    public GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks) {
        if (!(ingredient instanceof ArsSourcePortIngredient source)) {
            return null;
        }
        long amount = (long) source.getAmountRange().max() * (perTick ? Math.max(1, ticks) : 1);
        return amount <= 0 ? null : new GenericStack(SourceKey.KEY, amount);
    }

    @Override
    public boolean handles(AEKey key) {
        return key instanceof SourceKey;
    }

    @Override
    public boolean insert(Ae2PushContext context, AEKey key, long amount) {
        long remaining = amount;
        for (ArsSourcePortStorage storage : context.storages().getInputStorages(ArsSourcePortStorage.class)) {
            remaining -= storage.receive((int) Math.min(remaining, Integer.MAX_VALUE), context.simulate());
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pushOutputs(RecipeStorages storages, MEStorage me, IActionSource source) {
        for (ArsSourcePortStorage storage : storages.getOutputStorages(ArsSourcePortStorage.class)) {
            int stored = storage.getStored();
            if (stored <= 0) {
                continue;
            }
            long inserted = me.insert(SourceKey.KEY, stored, Actionable.MODULATE, source);
            if (inserted > 0) {
                storage.extract((int) inserted, false);
            }
        }
    }
}
