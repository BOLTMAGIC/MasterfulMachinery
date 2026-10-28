package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class Ae2ItemPortStorage implements MEStorage {
    private static final int INSERT_CHUNK = 16384;

    private final ItemPortHandler handler;

    public Ae2ItemPortStorage(ItemPortHandler handler) {
        this.handler = handler;
    }

    @Override
    public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (!(key instanceof AEItemKey item) || amount == 0) {
            return 0;
        }
        long accepted = Math.min(amount, handler.insertionCapacity(item.toStack(1)));
        if (mode == Actionable.SIMULATE) {
            return accepted;
        }
        long inserted = 0;
        while (inserted < accepted) {
            int chunk = (int) Math.min(accepted - inserted, INSERT_CHUNK);
            ItemStack remainder = handler.insertStackFast(item.toStack(chunk), false);
            int moved = chunk - remainder.getCount();
            if (moved <= 0) {
                break;
            }
            inserted += moved;
        }
        return inserted;
    }

    @Override
    public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (!(key instanceof AEItemKey item) || amount == 0) {
            return 0;
        }
        long remaining = amount;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!item.matches(handler.getStackInSlot(slot))) {
                continue;
            }
            ItemStack extracted = handler.extractItem(slot, (int) Math.min(remaining, Integer.MAX_VALUE), mode == Actionable.SIMULATE);
            remaining -= extracted.getCount();
        }
        return amount - remaining;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                out.add(AEItemKey.of(stack), handler.getActualCount(slot));
            }
        }
    }

    @Override
    public Component getDescription() {
        return Component.translatable("port.mm.item.title");
    }
}
