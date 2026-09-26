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
        rebind(toolStack);
    }

    /**
     * Re-reads this store's contents from a (possibly new) tool stack instance, without replacing this
     * handler object. Used on the client: vanilla replaces the held {@link ItemStack} instance whenever
     * it re-syncs (e.g. our own locked inventory slot receiving an update), which would otherwise leave
     * a store built once at menu-open time stuck showing stale counts. The server never needs to call
     * this outside construction; it stays authoritative via its own single, stable {@code toolStack}.
     */
    public void rebind(ItemStack newToolStack) {
        this.toolStack = newToolStack;
        CompoundTag tag = newToolStack.getTagElement(KEY);
        deserializeNBT(tag != null ? tag : new CompoundTag());
    }

    @Override
    protected void onContentsChanged(int slot) {
        toolStack.getOrCreateTag().put(KEY, serializeNBT());
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
