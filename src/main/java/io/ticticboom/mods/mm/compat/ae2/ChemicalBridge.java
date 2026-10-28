package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;

final class ChemicalBridge implements Ae2KeyBridge {

    @Override
    public String type() {
        return "chemical";
    }

    @Override
    public GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks) {
        if (!(ingredient instanceof MekanismChemicalPortIngredient chemical) || chemical.getChemical().isEmptyType() || chemical.getAmount() <= 0) {
            return null;
        }
        var key = MekanismKey.of(new ChemicalStack(chemical.getChemical(), 1));
        return key == null ? null : new GenericStack(key, chemical.getAmount());
    }

    @Override
    public boolean handles(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public boolean insert(Ae2PushContext context, AEKey key, long amount) {
        var chemical = (MekanismKey) key;
        var action = context.simulate() ? Action.SIMULATE : Action.EXECUTE;
        long remaining = amount;
        for (MekanismChemicalPortStorage storage : context.storages().getInputStorages(MekanismChemicalPortStorage.class)) {
            remaining -= storage.insert(chemical.withAmount(remaining), action).getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pushOutputs(RecipeStorages storages, MEStorage me, IActionSource source) {
        for (MekanismChemicalPortStorage storage : storages.getOutputStorages(MekanismChemicalPortStorage.class)) {
            var stack = storage.chemicalTank.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            var key = MekanismKey.of(stack);
            if (key == null) {
                continue;
            }
            long inserted = me.insert(key, stack.getAmount(), Actionable.MODULATE, source);
            if (inserted > 0) {
                storage.extract(inserted, Action.EXECUTE);
            }
        }
    }
}
