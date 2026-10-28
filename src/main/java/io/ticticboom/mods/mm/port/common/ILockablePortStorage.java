package io.ticticboom.mods.mm.port.common;

public interface ILockablePortStorage {
    boolean isLocked();

    void setLocked(boolean locked);

    void dump();
}
