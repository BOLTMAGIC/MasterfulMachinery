package io.ticticboom.mods.mm.tool;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The 54-slot block store carried by a {@link MultiblockToolItem}, backed by the tool's own NBT
 * (compound {@value #KEY}). Up to {@link #LIMIT} of a stackable item per slot; non-stackables stay at 1.
 * <p>
 * Serialization keeps the real count as an int (key {@value #COUNT_KEY}) alongside the vanilla
 * {@code ItemStack.save} data, since that save writes {@code Count} as a byte and would corrupt
 * counts above 127 (and wrap around above 255).
 */
public class ToolStore extends ItemStackHandler {
    public static final int SLOTS = 54;
    public static final int LIMIT = 512;
    private static final String KEY = "ToolStore";
    private static final String COUNT_KEY = "C";

    private ItemStack toolStack;

    public ToolStore(ItemStack toolStack) {
        super(SLOTS);
        // always bind, even in the (unexpected) case this is constructed with an empty stack -
        // onContentsChanged guards writes, and rebind() below is only ever called post-construction
        this.toolStack = toolStack;
        CompoundTag tag = toolStack.getTagElement(KEY);
        deserializeNBT(tag != null ? tag : new CompoundTag());
    }

    /**
     * Re-reads this store's contents from a (possibly new) tool stack instance, without replacing this
     * handler object. Used on the client: vanilla replaces the held {@link ItemStack} instance whenever
     * it re-syncs (e.g. our own locked inventory slot receiving an update), which would otherwise leave
     * a store built once at menu-open time stuck showing stale counts. The server never needs to call
     * this outside construction; it stays authoritative via its own single, stable {@code toolStack}.
     * <p>
     * Refuses to bind onto an empty stack (e.g. a predicted client-side click that briefly empties the
     * held slot): that would otherwise point this handler at the shared {@code ItemStack.EMPTY}
     * singleton, and the next {@link #onContentsChanged} would try to tag it. The store just keeps
     * showing its last known-good contents until a real (non-empty) stack comes back.
     */
    public void rebind(ItemStack newToolStack) {
        if (newToolStack.isEmpty()) {
            return;
        }
        this.toolStack = newToolStack;
        CompoundTag tag = newToolStack.getTagElement(KEY);
        deserializeNBT(tag != null ? tag : new CompoundTag());
    }

    /** False once the bound tool stack is gone (dropped, consumed, ...): every operation then refuses. */
    public boolean isToolStackPresent() {
        return !toolStack.isEmpty();
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (toolStack.isEmpty()) {
            return;
        }
        toolStack.getOrCreateTag().put(KEY, serializeNBT());
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isToolStackPresent()) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isToolStackPresent()) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return LIMIT;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return stack.isStackable() ? LIMIT : 1;
    }

    /** A tool can never store another Multiblock Tool (would allow nesting/duplication). */
    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !(stack.getItem() instanceof MultiblockToolItem);
    }

    @Override
    public @NotNull CompoundTag serializeNBT() {
        var nbt = new CompoundTag();
        var list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            var itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(itemTag);
            itemTag.putInt(COUNT_KEY, stack.getCount());
            list.add(itemTag);
        }
        nbt.put("Items", list);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : SLOTS);
        ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= stacks.size()) {
                continue;
            }
            ItemStack stack = ItemStack.of(itemTag);
            if (itemTag.contains(COUNT_KEY, Tag.TAG_INT)) {
                stack.setCount(itemTag.getInt(COUNT_KEY));
            }
            stacks.set(slot, stack);
        }
        onLoad();
    }
}
