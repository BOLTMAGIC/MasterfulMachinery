package io.ticticboom.mods.mm.cap.transfer;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** An MM energy storage as seen by NeoForge's transfer API. */
public final class LegacyEnergyHandler implements EnergyHandler {
    private final IEnergyStorage storage;
    private final LegacyJournal journal;

    public LegacyEnergyHandler(IEnergyStorage storage) {
        this.storage = storage;
        this.journal = new LegacyJournal(storage);
    }

    public IEnergyStorage storage() {
        return storage;
    }

    @Override
    public long getAmountAsLong() {
        return storage.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0 || !storage.canReceive()) {
            return 0;
        }
        int accepted = storage.receiveEnergy(amount, true);
        if (accepted <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return storage.receiveEnergy(accepted, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (amount <= 0 || !storage.canExtract()) {
            return 0;
        }
        int available = storage.extractEnergy(amount, true);
        if (available <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return storage.extractEnergy(available, false);
    }
}
