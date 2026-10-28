package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.Action;

final class ChemicalDrainer {

    private ChemicalDrainer() {
    }

    static boolean drain(IPortStorage storage, MEStorage network, IActionSource source) {
        if (!(storage instanceof MekanismChemicalPortStorage chemical)) {
            return false;
        }
        var stored = chemical.chemicalTank.getStack();
        if (stored.isEmpty()) {
            return false;
        }
        MekanismKey key = MekanismKey.of(stored);
        if (key == null) {
            return false;
        }
        long accepted = network.insert(key, stored.getAmount(), Actionable.SIMULATE, source);
        if (accepted > 0) {
            var extracted = chemical.extract(accepted, Action.EXECUTE);
            return network.insert(key, extracted.getAmount(), Actionable.MODULATE, source) > 0;
        }
        return false;
    }
}
