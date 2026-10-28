package io.ticticboom.mods.mm.port.mekanism.chemical;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.common.autoio.IPortTransfer;
import mekanism.api.Action;
import mekanism.api.chemical.IChemicalHandler;

import java.util.function.Supplier;

public final class ChemicalPortTransfers {

    private ChemicalPortTransfers() {
    }

    public static IPortTransfer chemicals(Supplier<IChemicalHandler> self) {
        return (level, pos, face, pull, ticks) -> {
            var other = level.getCapability(MekCapabilities.CHEMICAL, pos, face);
            if (other == null) {
                return;
            }
            if (pull) {
                moveChemicals(other, self.get());
            } else {
                moveChemicals(self.get(), other);
            }
        };
    }

    public static void moveChemicals(IChemicalHandler from, IChemicalHandler to) {
        for (int tank = 0; tank < from.getChemicalTanks(); tank++) {
            var stored = from.getChemicalInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }
            var available = from.extractChemical(tank, stored.getAmount(), Action.SIMULATE);
            if (available.isEmpty()) {
                continue;
            }
            var notAccepted = to.insertChemical(available, Action.SIMULATE);
            long amount = available.getAmount() - notAccepted.getAmount();
            if (amount <= 0) {
                continue;
            }
            var extracted = from.extractChemical(tank, amount, Action.EXECUTE);
            if (extracted.isEmpty()) {
                continue;
            }
            var leftover = to.insertChemical(extracted, Action.EXECUTE);
            if (!leftover.isEmpty()) {
                from.insertChemical(leftover, Action.EXECUTE);
            }
        }
    }
}
