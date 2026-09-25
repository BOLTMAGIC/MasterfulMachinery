package io.ticticboom.mods.mm.compat.appmek;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;

/**
 * Mekanism gas / infuse / pigment / slurry ports -> AE2 through Applied Mekanistics.
 * Only loaded when Applied Mekanistics and Mekanism are present.
 */
public final class ChemicalDrainer {

    private ChemicalDrainer() {
    }

    public static void drain(IPortStorage storage, MEStorage network, IActionSource source) {
        if (!(storage instanceof MekanismChemicalPortStorage<?, ?> chemical)) {
            return;
        }
        ChemicalStack<?> stored = chemical.chemicalTank.getStack();
        if (stored.isEmpty()) {
            return;
        }
        MekanismKey key = MekanismKey.of(stored);
        if (key == null) {
            return;
        }
        // ask first: a network without chemical storage accepts nothing, and the chemical stays in the port
        long accepted = network.insert(key, stored.getAmount(), Actionable.SIMULATE, source);
        if (accepted > 0) {
            ChemicalStack<?> extracted = chemical.extract(accepted, Action.EXECUTE);
            network.insert(key, extracted.getAmount(), Actionable.MODULATE, source);
        }
    }
}
