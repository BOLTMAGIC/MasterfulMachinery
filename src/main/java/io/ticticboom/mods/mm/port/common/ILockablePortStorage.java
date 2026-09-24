package io.ticticboom.mods.mm.port.common;

/**
 * Storage whose tanks can be locked to their current contents and dumped (voided) from the GUI.
 */
public interface ILockablePortStorage {
    boolean isLocked();

    /**
     * Locking remembers the type currently held by the port; an empty port remembers the first
     * type that enters it while locked. Unlocking forgets the remembered type.
     */
    void setLocked(boolean locked);

    /**
     * Permanently deletes all contents. Remembered lock types are kept.
     */
    void dump();
}
