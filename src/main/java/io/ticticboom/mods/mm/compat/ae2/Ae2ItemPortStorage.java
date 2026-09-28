package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;

/** AE2's direct Pattern Provider target for an item port. */
public final class Ae2ItemPortStorage implements MEStorage {
    private static final int INSERT_CHUNK = 16384;

    private final ItemPortHandler handler;
    private final Component description;

    public Ae2ItemPortStorage(ItemPortStorage port, Component description) {
        this.handler = port.getHandler();
        this.description = description;
    }

    public static boolean isStorageCapability(Capability<?> capability) {
        return capability == Capabilities.STORAGE;
    }

    @Override
    public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (!(key instanceof AEItemKey item) || amount == 0) return 0;

        ItemStack template = item.toStack(1);
        long accepted = Math.min(amount, handler.insertionCapacity(template));
        if (mode == Actionable.MODULATE) {
            long inserted = 0;
            while (inserted < accepted) {
                int chunk = (int) Math.min(accepted - inserted, INSERT_CHUNK);
                ItemStack remainder = handler.insertStackFast(item.toStack(chunk), false);
                int moved = chunk - remainder.getCount();
                if (moved <= 0) break;
                inserted += moved;
            }
            accepted = inserted;
        }
        return accepted;
    }

    @Override
    public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (!(key instanceof AEItemKey item) || amount == 0) return 0;

        long remaining = amount;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!item.matches(handler.getStackInSlot(slot))) continue;
            ItemStack extracted = handler.extractItem(slot, (int) Math.min(remaining, Integer.MAX_VALUE), mode.isSimulate());
            remaining -= extracted.getCount();
        }
        return amount - remaining;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(AEItemKey.of(stack), handler.getActualCount(slot));
        }
    }

    @Override
    public Component getDescription() {
        return description;
    }
}
