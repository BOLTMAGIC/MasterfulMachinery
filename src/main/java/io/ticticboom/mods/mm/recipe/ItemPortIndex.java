package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A controller-owned view of item ports, refreshed only after a handler changes. */
public final class ItemPortIndex {
    private final List<ItemPortStorage> ports = new ArrayList<>();
    private final long[] revisions;
    private final long[] observedRevisions;
    private final Map<Item, List<ItemPortStorage>> portsByItem = new HashMap<>();
    private List<ItemPortStorage> portsWithEmptySlots = List.of();
    private boolean initialized;
    private boolean observed;

    public ItemPortIndex(List<IPortStorage> storages) {
        for (IPortStorage storage : storages) {
            if (storage instanceof ItemPortStorage itemPort) ports.add(itemPort);
        }
        revisions = new long[ports.size()];
        observedRevisions = new long[ports.size()];
    }

    public List<ItemPortStorage> forItem(Item item) {
        refresh();
        return portsByItem.getOrDefault(item, List.of());
    }

    public List<ItemPortStorage> withEmptySlots() {
        refresh();
        return portsWithEmptySlots;
    }

    /** Poll the handlers' change counters without walking any item slots. */
    public boolean changedSinceLastPoll() {
        boolean changed = !observed;
        observed = true;
        for (int i = 0; i < ports.size(); i++) {
            long revision = ports.get(i).getHandler().contentRevision();
            if (observedRevisions[i] != revision) changed = true;
            observedRevisions[i] = revision;
        }
        return changed;
    }

    private void refresh() {
        boolean changed = !initialized;
        for (int i = 0; i < ports.size(); i++) {
            long revision = ports.get(i).getHandler().contentRevision();
            if (revisions[i] != revision) changed = true;
            revisions[i] = revision;
        }
        if (!changed) return;
        initialized = true;
        portsByItem.clear();
        List<ItemPortStorage> empty = new ArrayList<>();
        for (ItemPortStorage port : ports) {
            var handler = port.getHandler();
            for (Item item : handler.indexedItemTypes()) {
                portsByItem.computeIfAbsent(item, ignored -> new ArrayList<>()).add(port);
            }
            if (handler.hasEmptySlots()) empty.add(port);
        }
        portsWithEmptySlots = List.copyOf(empty);
    }
}
