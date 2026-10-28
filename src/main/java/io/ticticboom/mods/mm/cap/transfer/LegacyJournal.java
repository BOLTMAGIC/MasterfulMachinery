package io.ticticboom.mods.mm.cap.transfer;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Rolls a legacy handler back when the transaction that changed it is aborted. The handler is changed for real as soon
 * as it is asked to; a snapshot taken before the first change in each transaction is what an abort restores.
 */
final class LegacyJournal extends SnapshotJournal<Object> {
    private static final Object NONE = new Object();
    private static boolean warned;

    private final Object handler;

    LegacyJournal(Object handler) {
        this.handler = handler;
    }

    /** Call before every change the transaction makes. */
    void beforeChange(TransactionContext transaction) {
        updateSnapshots(transaction);
    }

    @Override
    protected Object createSnapshot() {
        if (handler instanceof TransferSnapshot snapshotting) {
            return snapshotting.createSnapshot();
        }
        if (!warned) {
            warned = true;
            Ref.LOG.warn("{} cannot be rolled back; aborted transfers into it are kept", handler.getClass().getName());
        }
        return NONE;
    }

    @Override
    protected void revertToSnapshot(Object snapshot) {
        if (snapshot != NONE && handler instanceof TransferSnapshot snapshotting) {
            snapshotting.restoreSnapshot(snapshot);
        }
    }
}
