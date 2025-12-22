package io.ticticboom.mods.mm.port.fluid;

import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.*;

public class FluidPortRouting {
    public static int distributeFillThrough(FluidStack stack, int requestedAmount, FluidPortStorage source, Map<BlockPos, FluidPortStorage> candidates) {
        if (requestedAmount <= 0) return 0;
        int remaining = requestedAmount;

        Map<Integer, List<Map.Entry<BlockPos, FluidPortStorage>>> grouped = new HashMap<>();
        for (var entry : candidates.entrySet()) {
            var storage = entry.getValue();
            int space = storage.getWrappedHandler().fill(new FluidStack(stack, Integer.MAX_VALUE), FluidAction.SIMULATE);
            if (space <= 0) continue;
            int prio = storage.getPriority();
            grouped.computeIfAbsent(prio, k -> new ArrayList<>()).add(entry);
        }

        List<Integer> priorities = new ArrayList<>(grouped.keySet());
        priorities.sort(Comparator.reverseOrder());

        for (int prio : priorities) {
            var list = grouped.get(prio);
            // snapshot remaining for comparator
            int remForSort = remaining;
            list.sort((a, b) -> {
                int sa = a.getValue().getWrappedHandler().fill(new FluidStack(stack, Integer.MAX_VALUE), FluidAction.SIMULATE);
                int sb = b.getValue().getWrappedHandler().fill(new FluidStack(stack, Integer.MAX_VALUE), FluidAction.SIMULATE);
                if (sa != sb) return Integer.compare(sb, sa);
                long pa = a.getKey().asLong();
                long pb = b.getKey().asLong();
                return Long.compare(pa, pb);
            });

            for (var entry : list) {
                if (remaining <= 0) break;
                var target = entry.getValue();
                int canAccept = target.getWrappedHandler().fill(new FluidStack(stack, remaining), FluidAction.SIMULATE);
                if (canAccept <= 0) continue;
                int accepted = target.getWrappedHandler().fill(new FluidStack(stack, canAccept), FluidAction.EXECUTE);
                if (accepted > 0) {
                    // remove from source: drain accepted amount
                    source.getHandler().drain(accepted, FluidAction.EXECUTE);
                    remaining -= accepted;
                }
            }
            if (remaining <= 0) break;
        }

        return requestedAmount - remaining;
    }
}
