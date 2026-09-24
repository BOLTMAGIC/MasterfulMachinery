package io.ticticboom.mods.mm.port.mekanism.chemical;

import io.ticticboom.mods.mm.port.common.autoio.IPortTransfer;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Mekanism chemical transfer for {@link io.ticticboom.mods.mm.port.common.autoio.PortAutoIO}.
 * Kept apart from PortTransfers so Mekanism classes are only loaded when Mekanism ports exist.
 */
public final class ChemicalPortTransfers {

    private ChemicalPortTransfers() {
    }

    public static <C extends Chemical<C>, S extends ChemicalStack<C>> IPortTransfer chemicals(
            Capability<? extends IChemicalHandler<C, S>> cap, IChemicalHandler<C, S> self) {
        return (neighbor, face, pull, ticks) -> neighbor.getCapability(cap, face).ifPresent(other -> {
            if (pull) {
                moveChemicals(other, self);
            } else {
                moveChemicals(self, other);
            }
        });
    }

    public static <C extends Chemical<C>, S extends ChemicalStack<C>> void moveChemicals(IChemicalHandler<C, S> from, IChemicalHandler<C, S> to) {
        for (int tank = 0; tank < from.getTanks(); tank++) {
            S stored = from.getChemicalInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }
            S available = from.extractChemical(tank, stored.getAmount(), Action.SIMULATE);
            if (available.isEmpty()) {
                continue;
            }
            S notAccepted = to.insertChemical(available, Action.SIMULATE);
            long amount = available.getAmount() - notAccepted.getAmount();
            if (amount <= 0) {
                continue;
            }
            to.insertChemical(from.extractChemical(tank, amount, Action.EXECUTE), Action.EXECUTE);
        }
    }
}
