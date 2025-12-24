package io.ticticboom.mods.mm.port.energy;

import net.minecraft.core.BlockPos;

import java.util.*;

public class EnergyPortRouting {
    public static int distributeFillThrough(int requestedAmount, EnergyPortStorage source, Map<BlockPos, EnergyPortStorage> candidates) {
        if (requestedAmount <= 0) return 0;
        int remaining = requestedAmount;

        Map<Integer, List<Map.Entry<BlockPos, EnergyPortStorage>>> grouped = new HashMap<>();
        for (var entry : candidates.entrySet()) {
            var storage = entry.getValue();
            int canReceive = storage.internalInsert(remaining, true);
            if (canReceive <= 0) continue;
            int prio = storage.getPriority();
            grouped.computeIfAbsent(prio, k -> new ArrayList<>()).add(entry);
        }

        List<Integer> priorities = new ArrayList<>(grouped.keySet());
        priorities.sort(Comparator.reverseOrder());

        for (int prio : priorities) {
            var list = grouped.get(prio);
            int remForSort = remaining;
            list.sort((a, b) -> {
                int sa = a.getValue().internalInsert(remForSort, true);
                int sb = b.getValue().internalInsert(remForSort, true);
                if (sa != sb) return Integer.compare(sb, sa);
                long pa = a.getKey().asLong();
                long pb = b.getKey().asLong();
                return Long.compare(pa, pb);
            });

            for (var entry : list) {
                if (remaining <= 0) break;
                var target = entry.getValue();
                int canAccept = target.internalInsert(remaining, true);
                if (canAccept <= 0) continue;
                int accepted = target.internalInsert(canAccept, false);
                if (accepted > 0) {
                    source.internalExtract(accepted, false);
                    remaining -= accepted;
                }
            }
            if (remaining <= 0) break;
        }

        return requestedAmount - remaining;
    }
}
