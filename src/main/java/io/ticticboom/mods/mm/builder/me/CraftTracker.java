package io.ticticboom.mods.mm.builder.me;

import io.ticticboom.mods.mm.Ref;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The crafts the Multiblock Tool asked a player's ME network for, in memory only: a relog or restart forgets them (AE2
 * crafts on, and the next right-click finds the items in the network). Handles are advanced on the server thread every
 * {@value #INTERVAL} ticks and never waited on. An item with a craft on its way counts as coming, never as missing.
 */
@Mod.EventBusSubscriber(modid = Ref.ID)
public final class CraftTracker {
    private static final int INTERVAL = 10;
    private static final Map<UUID, Orders> ORDERS = new HashMap<>();

    private CraftTracker() {
    }

    /** Every craft of one item for a player: requests for the same item are merged here. */
    public static final class Order {
        private final Item item;
        private final List<CraftHandle> handles = new ArrayList<>();
        /** Failures already told to the player. */
        private final Set<CraftHandle> reported = Collections.newSetFromMap(new IdentityHashMap<>());

        private Order(Item item) {
            this.item = item;
        }

        public Item item() {
            return item;
        }

        /** Amount requested, failed crafts left out. */
        public int requested() {
            return handles.stream().filter(h -> h.state() != CraftHandle.State.FAILED).mapToInt(CraftHandle::amount).sum();
        }

        /** Amount still calculating or crafting. */
        public int active() {
            return handles.stream().filter(h -> h.state().active()).mapToInt(CraftHandle::amount).sum();
        }

        /** Amount already crafted into the network. */
        public int done() {
            return handles.stream().filter(h -> h.state() == CraftHandle.State.DONE).mapToInt(CraftHandle::amount).sum();
        }

        /** The first failed craft of this item, or null. */
        public @Nullable CraftHandle failed() {
            return handles.stream().filter(h -> h.state() == CraftHandle.State.FAILED).findFirst().orElse(null);
        }
    }

    private static final class Orders {
        final Map<Item, Order> byItem = new LinkedHashMap<>();
        boolean readyTold;
    }

    /** How many of item are on their way for player (calculating or crafting). */
    public static int active(Player player, Item item) {
        Orders orders = ORDERS.get(player.getUUID());
        Order order = orders == null ? null : orders.byItem.get(item);
        return order == null ? 0 : order.active();
    }

    /**
     * Follows a new craft for player, merged with the item's earlier ones; the item's failed crafts are dropped (this
     * is the new attempt). A craft that failed at once is not told again by {@link #update}: its caller reports it.
     */
    public static void add(Player player, CraftHandle handle) {
        Orders orders = ORDERS.computeIfAbsent(player.getUUID(), id -> new Orders());
        Order order = orders.byItem.computeIfAbsent(handle.item(), Order::new);
        order.handles.removeIf(h -> h.state() == CraftHandle.State.FAILED);
        order.reported.clear();
        order.handles.add(handle);
        if (handle.state() == CraftHandle.State.FAILED) {
            order.reported.add(handle);
        }
        orders.readyTold = false;
    }

    /** Player's orders, in request order (for the HUD); empty when none. */
    public static List<Order> orders(Player player) {
        Orders orders = ORDERS.get(player.getUUID());
        return orders == null ? List.of() : List.copyOf(orders.byItem.values());
    }

    /** Forgets player's crafts (a build started with everything on hand); AE2 still finishes any that run. */
    public static void clear(Player player) {
        ORDERS.remove(player.getUUID());
    }

    /**
     * Advances player's crafts once: a newly failed one is told in the action bar (once), and when all have finished
     * without failing the player is told to right-click again. The build itself never starts on its own.
     */
    public static void update(Player player) {
        Orders orders = ORDERS.get(player.getUUID());
        if (orders == null) {
            return;
        }
        boolean active = false;
        boolean failed = false;
        for (Order order : orders.byItem.values()) {
            for (CraftHandle handle : order.handles) {
                handle.update();
                CraftHandle.State state = handle.state();
                active |= state.active();
                if (state == CraftHandle.State.FAILED) {
                    failed = true;
                    if (order.reported.add(handle)) {
                        player.displayClientMessage(entry(handle.item(), handle.amount(), handle.failure()).withStyle(ChatFormatting.YELLOW), true);
                    }
                }
            }
        }
        if (!active && !failed && !orders.readyTold) {
            orders.readyTold = true;
            player.displayClientMessage(Component.translatable("message.mm.tool.craft.ready").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** "Oak Planks ×12". */
    public static MutableComponent amount(Item item, int count) {
        return Component.empty().append(item.getDescription()).append(Component.literal(" ×" + count));
    }

    /** "Oak Planks ×12: reason". */
    public static MutableComponent entry(Item item, int count, @Nullable Component reason) {
        Component why = reason != null ? reason : Component.translatable("message.mm.tool.craft.error");
        return Component.translatable("message.mm.tool.craft.entry", amount(item, count), why);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ORDERS.isEmpty() || event.getServer().getTickCount() % INTERVAL != 0) {
            return;
        }
        // players not in the list (offline, or a test's fake player) are left alone; logging out drops them
        for (UUID id : List.copyOf(ORDERS.keySet())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                update(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ORDERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ORDERS.clear();
    }
}
