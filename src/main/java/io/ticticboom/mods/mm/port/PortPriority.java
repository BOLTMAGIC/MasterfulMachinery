package io.ticticboom.mods.mm.port;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Utilities for applying output-port priority consistently across port types. */
public final class PortPriority {
    private PortPriority() {
    }

    /**
     * Returns a copy ordered from highest priority to lowest priority.
     * The storage UID keeps equal-priority ports deterministic without changing
     * the storage list owned by the controller.
     */
    public static <T extends IPortStorage> List<T> orderedOutputs(List<T> storages) {
        var ordered = new ArrayList<>(storages);
        ordered.sort(Comparator.comparingInt(IPortStorage::getPriority).reversed()
                .thenComparing(storage -> storage.getStorageUid().toString()));
        return ordered;
    }
}
