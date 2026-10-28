package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;

final class EnergyBridge implements Ae2KeyBridge {

    private final FluxKey key = FluxKey.of(EnergyType.FE);

    @Override
    public String type() {
        return "energy";
    }

    @Override
    public GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks) {
        if (!(ingredient instanceof EnergyPortIngredient energy) || energy.getAmount() <= 0) {
            return null;
        }
        return new GenericStack(key, energy.getAmount());
    }

    @Override
    public boolean handles(AEKey key) {
        return key instanceof FluxKey flux && flux.getEnergyType() == EnergyType.FE;
    }

    @Override
    public boolean insert(Ae2PushContext context, AEKey key, long amount) {
        long remaining = amount;
        for (EnergyPortStorage storage : context.storages().getInputStorages(EnergyPortStorage.class)) {
            remaining -= storage.internalInsert(remaining, context.simulate());
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pushOutputs(RecipeStorages storages, MEStorage me, IActionSource source) {
        for (EnergyPortStorage storage : storages.getOutputStorages(EnergyPortStorage.class)) {
            long stored = storage.getStoredEnergy();
            if (stored <= 0) {
                continue;
            }
            long inserted = me.insert(key, stored, Actionable.MODULATE, source);
            if (inserted > 0) {
                storage.internalExtract(inserted, false);
            }
        }
    }
}
