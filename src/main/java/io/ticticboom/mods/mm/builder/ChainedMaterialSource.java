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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ChainedMaterialSource implements MaterialSource, ItemSink {
    private final ToolStore store;
    private final Player player;
    private final ToolEnergy energy;
    @Nullable
    private final MeAccess me;
    private boolean meLost;

    public ChainedMaterialSource(ToolStore store, Player player, ToolEnergy energy) {
        this(store, player, energy, null);
    }

    public ChainedMaterialSource(ToolStore store, Player player, ToolEnergy energy, @Nullable MeAccess me) {
        this.store = store;
        this.player = player;
        this.energy = energy;
        this.me = me;
    }

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

    private boolean meHas(Item item) {
        return meStock(item) > 0 && !me.extract(item, 1, true).isEmpty();
    }

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
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            addBuildable(items, stack);
        }
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

    public long available(Item item) {
        if (free()) {
            return Long.MAX_VALUE;
        }
        if (item == Items.AIR) {
            return 0;
        }
        long count = 0;
        if (toolCarried()) {
            for (int i = 0; i < store.getSlots(); i++) {
                count += buildableCount(store.getStackInSlot(i), item);
            }
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            count += buildableCount(stack, item);
        }
        return count + meStock(item);
    }

    private static int buildableCount(ItemStack stack, Item item) {
        return PlayerMaterials.buildable(stack, item) ? stack.getCount() : 0;
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

    @Override
    public boolean gone() {
        return !free() && !toolCarried();
    }

    @Override
    public boolean ready() {
        return !(player.containerMenu instanceof MultiblockToolMenu);
    }

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

    private int storeSlot(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR || !toolCarried()) {
            return -1;
        }
        int tagged = -1;
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack stack = store.getStackInSlot(i);
            if (PlayerMaterials.buildable(stack, item)) {
                if (stack.getComponentsPatch().isEmpty()) {
                    return i;
                }
                if (tagged < 0) {
                    tagged = i;
                }
            }
        }
        return tagged;
    }

    private boolean toolCarried() {
        if (!store.isToolStackPresent()) {
            return false;
        }
        ItemStack tool = store.toolStack();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack == tool) {
                return true;
            }
        }
        return player.getOffhandItem() == tool;
    }
}
