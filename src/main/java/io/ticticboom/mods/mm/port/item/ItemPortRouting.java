package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.Ref;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import java.util.*;

public class ItemPortRouting {

    /**
     * Attempts to distribute up to requestedAmount of the given item from source storage to the candidate outputs
     * following priority descending (10..0) and filling outputs in order until empty or no space.
     * Returns amount actually transferred.
     */
    public static int distributeFillThrough(Item item, int requestedAmount, ItemPortStorage source, Map<BlockPos, ItemPortStorage> candidates) {
        if (requestedAmount <= 0) return 0;
        int remaining = requestedAmount;

        // Build list of candidates with available space > 0
        Map<Integer, List<Map.Entry<BlockPos, ItemPortStorage>>> grouped = new HashMap<>();
        for (var entry : candidates.entrySet()) {
            var storage = entry.getValue();
            int space = storage.canInsert(item, Integer.MAX_VALUE);
            if (space <= 0) continue;
            int prio = storage.getPriority();
            grouped.computeIfAbsent(prio, k -> new ArrayList<>()).add(entry);
        }

        // process priorities descending
        List<Integer> priorities = new ArrayList<>(grouped.keySet());
        priorities.sort(Comparator.reverseOrder());

        // process each priority group
        for (int prio : priorities) {
            var list = grouped.get(prio);
            list.sort((a, b) -> {
                int sa = a.getValue().canInsert(item, Integer.MAX_VALUE);
                int sb = b.getValue().canInsert(item, Integer.MAX_VALUE);
                if (sa != sb) return Integer.compare(sb, sa); // desc
                long pa = a.getKey().asLong();
                long pb = b.getKey().asLong();
                return Long.compare(pa, pb);
            });

            for (var entry : list) {
                if (remaining <= 0) break;
                var target = entry.getValue();
                int remainingNotInsertedIfTried = target.canInsert(item, remaining);
                int available = remaining - remainingNotInsertedIfTried; // amount that can be accepted
                if (available <= 0) continue;
                int toTry = Math.min(remaining, available);

                // extract from source first
                int remainingAfterExtract = source.extract(s -> s.getItem() == item, toTry);
                int actuallyExtracted = toTry - remainingAfterExtract;
                if (actuallyExtracted <= 0) continue;

                int notInserted = target.insert(item, actuallyExtracted);
                int actuallyMoved = actuallyExtracted - notInserted;

                // if some couldn't be inserted, put them back into source (best-effort)
                if (notInserted > 0) {
                    int returned = source.insert(item, notInserted);
                    if (returned > 0) {
                        // logging suppressed
                    }
                }

                remaining -= actuallyMoved;

                // debug removed
            }
            if (remaining <= 0) break;
        }

        return requestedAmount - remaining;
    }
}
