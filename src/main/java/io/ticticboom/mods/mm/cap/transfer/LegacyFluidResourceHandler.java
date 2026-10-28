package io.ticticboom.mods.mm.cap.transfer;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * An MM fluid handler as seen by NeoForge's transfer API. IFluidHandler fills and drains the handler as a whole
 * rather than one tank, so a tank-indexed insert or extract goes to the handler too, and only the amount it reports
 * moved counts.
 */
public final class LegacyFluidResourceHandler implements ResourceHandler<FluidResource> {
    private final IFluidHandler handler;
    private final LegacyJournal journal;

    public LegacyFluidResourceHandler(IFluidHandler handler) {
        this.handler = handler;
        this.journal = new LegacyJournal(handler);
    }

    public IFluidHandler handler() {
        return handler;
    }

    @Override
    public int size() {
        return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(handler.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        int accepted = handler.fill(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return handler.fill(resource.toStack(accepted), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        int available = handler.drain(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE).getAmount();
        if (available <= 0) {
            return 0;
        }
        journal.beforeChange(transaction);
        return handler.drain(resource.toStack(available), IFluidHandler.FluidAction.EXECUTE).getAmount();
    }
}
