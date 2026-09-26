package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The multiblock tool's source: blocks come from the tool's own store first, then the player's inventory, then the
 * ME network the tool is bound to (when given one); energy comes from the tool. Nothing is taken or paid in creative.
 * Refunds, and a sink's items (dismantling), never go to the network: tool store, then inventory, then the ground.
 */
public final class ChainedMaterialSource implements MaterialSource, ItemSink {
    private final ToolStore store;
    private final Player player;
    private final ToolEnergy energy;
    @Nullable
    private final MeAccess me;
    /** The network could not be used at some tick of the job (gone, unpowered, access withdrawn). */
    private boolean meLost;

    public ChainedMaterialSource(ToolStore store, Player player, ToolEnergy energy) {
        this(store, player, energy, null);
    }

    /** @param me the tool's reachable ME network, or null to use the store and inventory only */
    public ChainedMaterialSource(ToolStore store, Player player, ToolEnergy energy, @Nullable MeAccess me) {
        this.store = store;
        this.player = player;
        this.energy = energy;
        this.me = me;
    }

    /** The ME network this source may draw from, or null. */
    public @Nullable MeAccess me() {
        return me;
    }

    @Override
    public boolean has(Block block) {
        if (free()) {
            return true;
        }
        return storeSlot(block) >= 0 || PlayerMaterials.has(player, block) || meHas(block.asItem());
    }

    /**
     * The network can give one item right now. The snapshot rules most items out; a simulated extraction confirms the
     * rest, since the job relies on {@link #take} delivering what {@code has} promised (the snapshot may lag a tick).
     */
    private boolean meHas(Item item) {
        return meStock(item) > 0 && !me.extract(item, 1, true).isEmpty();
    }

    /**
     * {@link #has} frozen now, as a set lookup: for planning and previews that ask about many blocks at once. Later
     * changes to the store or inventory are not seen; the store is as read at construction or the last
     * {@link #beginTick}. The network is asked once per item (from its stock snapshot), never per position.
     */
    public Predicate<Block> snapshot() {
        if (free()) {
            return block -> true;
        }
        Set<Item> items = new HashSet<>();
        if (toolCarried()) {
            for (int i = 0; i < store.getSlots(); i++) {
                addBuildable(items, store.getStackInSlot(i));
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            addBuildable(items, stack);
        }
        // empty stacks and blocks without an item both map to air
        items.remove(Items.AIR);
        if (me == null) {
            return block -> items.contains(block.asItem());
        }
        Map<Item, Boolean> inMe = new HashMap<>();
        return block -> {
            Item item = block.asItem();
            return items.contains(item) || inMe.computeIfAbsent(item, i -> meStock(i) > 0);
        };
    }

    private static void addBuildable(Set<Item> items, ItemStack stack) {
        if (PlayerMaterials.buildable(stack, stack.getItem())) {
            items.add(stack.getItem());
        }
    }

    @Override
    public ItemStack take(Block block) {
        if (free()) {
            return ItemStack.EMPTY;
        }
        int slot = storeSlot(block);
        if (slot >= 0) {
            return store.extractItem(slot, 1, false);
        }
        ItemStack taken = PlayerMaterials.take(player, block);
        if (taken != null) {
            return taken;
        }
        return meStock(block.asItem()) > 0 ? me.extract(block.asItem(), 1, false) : ItemStack.EMPTY;
    }

    /** Stock of item in the bound network: 0 without one, while it is unreachable, or while the tool is not carried. */
    private long meStock(Item item) {
        if (me == null || item == Items.AIR || !me.reachable() || !toolCarried()) {
            return 0;
        }
        return me.stock(item);
    }

    @Override
    public void refund(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack leftover = stack;
        if (toolCarried()) {
            leftover = ItemHandlerHelper.insertItemStacked(store, stack, false);
        }
        if (!leftover.isEmpty()) {
            // drops whatever the inventory cannot hold
            player.getInventory().placeItemBackInInventory(leftover);
        }
    }

    @Override
    public void insert(ItemStack stack) {
        refund(stack);
    }

    @Override
    public boolean payEnergy(int fe) {
        if (free() || fe <= 0) {
            return true;
        }
        if (!toolCarried() || energy.getEnergyStored() < fe) {
            return false;
        }
        energy.extractEnergy(fe, false);
        return true;
    }

    @Override
    public void refundEnergy(int fe) {
        if (free() || fe <= 0 || !toolCarried()) {
            return;
        }
        energy.restore(fe);
    }

    /** The tool left the player (dropped, stored away or split off); creative builds need nothing from it. */
    @Override
    public boolean gone() {
        return !free() && !toolCarried();
    }

    /** Waits while the tool's own store is open, so the menu and the job never write over each other. */
    @Override
    public boolean ready() {
        return !(player.containerMenu instanceof MultiblockToolMenu);
    }

    /**
     * Re-reads the store from the tool once per job tick. Within a tick only this store writes the tool's store NBT:
     * the menu (the other writer) is closed whenever {@link #ready()} lets the job run. The network is looked up again
     * too, with a fresh stock snapshot.
     */
    @Override
    public void beginTick() {
        if (toolCarried()) {
            store.reload();
        }
        if (me != null) {
            me.refresh();
            if (!me.reachable()) {
                meLost = true;
            }
        }
    }

    @Override
    public @Nullable Component summaryNote() {
        return meLost ? Component.translatable("message.mm.tool.me_lost") : null;
    }

    @Override
    public boolean free() {
        return player.getAbilities().instabuild;
    }

    /**
     * The store slot to take block's item from (plain stacks before tagged ones, never block entity data, like the
     * inventory), or -1.
     */
    private int storeSlot(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR || !toolCarried()) {
            return -1;
        }
        int tagged = -1;
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack stack = store.getStackInSlot(i);
            if (PlayerMaterials.buildable(stack, item)) {
                if (!stack.hasTag()) {
                    return i;
                }
                if (tagged < 0) {
                    tagged = i;
                }
            }
        }
        return tagged;
    }

    /** The tool still exists and is still on the player (not dropped, stored away or split off). */
    private boolean toolCarried() {
        if (!store.isToolStackPresent()) {
            return false;
        }
        ItemStack tool = store.toolStack();
        var inventory = player.getInventory();
        for (var compartment : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : compartment) {
                if (stack == tool) {
                    return true;
                }
            }
        }
        return false;
    }
}
