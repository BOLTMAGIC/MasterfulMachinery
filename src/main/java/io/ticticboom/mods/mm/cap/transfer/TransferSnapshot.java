package io.ticticboom.mods.mm.cap.transfer;

/**
 * A handler whose contents can be copied and put back. NeoForge's transfer API runs every insert and extract inside a
 * transaction that may be rolled back (that is how it simulates), so MM's handlers, which change their contents
 * straight away, need this to undo what an aborted transaction did.
 */
public interface TransferSnapshot {
    Object createSnapshot();

    void restoreSnapshot(Object snapshot);
}
