package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;

import java.util.ArrayList;
import java.util.List;

/** A gateway's snapshot is the snapshots of the port handlers it forwards to. */
final class GatewaySnapshots {
    private record Entry(TransferSnapshot handler, Object snapshot) {
    }

    private GatewaySnapshots() {
    }

    static Object create(List<?> handlers) {
        var entries = new ArrayList<Entry>(handlers.size());
        for (Object handler : handlers) {
            if (handler instanceof TransferSnapshot snapshotting) {
                entries.add(new Entry(snapshotting, snapshotting.createSnapshot()));
            }
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    static void restore(Object snapshot) {
        for (Entry entry : (List<Entry>) snapshot) {
            entry.handler().restoreSnapshot(entry.snapshot());
        }
    }
}
