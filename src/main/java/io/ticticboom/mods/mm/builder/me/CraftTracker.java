package io.ticticboom.mods.mm.builder.me;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.ticticboom.mods.mm.Ref;
import net.neoforged.neoforge.network.PacketDistributor;
import io.ticticboom.mods.mm.net.packet.ToolHudPkt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

@EventBusSubscriber(modid = Ref.ID)
public final class CraftTracker {
    private static final int INTERVAL = 10;
    private static final int DONE_GRACE = 2;
    private static final int KEEP_FINISHED = 30;
    private static final int RESEND = 2;
    private static final Map<UUID, Orders> ORDERS = new HashMap<>();

    private CraftTracker() {
    }

    public static final class Order {
        private final Item item;
        private final List<CraftHandle> handles = new ArrayList<>();
        private final Set<CraftHandle> reported = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<CraftHandle, Integer> unseen = new IdentityHashMap<>();
        private final Set<CraftHandle> finished = Collections.newSetFromMap(new IdentityHashMap<>());

        private Order(Item item) {
            this.item = item;
        }

        public Item item() {
            return item;
        }

        public int requested() {
            return handles.stream().filter(h -> h.state() != CraftHandle.State.FAILED).mapToInt(CraftHandle::amount).sum();
        }

        public int active() {
            return handles.stream().filter(h -> h.state().active()).mapToInt(CraftHandle::amount).sum();
        }

        public int done() {
            return handles.stream().filter(h -> h.state() == CraftHandle.State.DONE).mapToInt(CraftHandle::amount).sum();
        }

        public int coming() {
            int unseenDone = unseen.entrySet().stream().filter(e -> e.getValue() < DONE_GRACE).mapToInt(e -> e.getKey().amount()).sum();
            return active() + unseenDone;
        }

        public @Nullable CraftHandle failed() {
            return handles.stream().filter(h -> h.state() == CraftHandle.State.FAILED).findFirst().orElse(null);
        }
    }

    private static final class Orders {
        Player player;
        final Map<Item, Order> byItem = new LinkedHashMap<>();
        int finishedPolls;
        HudState sent = HudState.NONE;
        int sentPolls;

        Orders(Player player) {
            this.player = player;
        }
    }

    public static int active(Player player, Item item) {
        Order order = order(player, item);
        return order == null ? 0 : order.active();
    }

    public static int coming(Player player, Item item) {
        Order order = order(player, item);
        return order == null ? 0 : order.coming();
    }

    public static void seen(Player player, Item item) {
        Order order = order(player, item);
        if (order != null) {
            order.unseen.clear();
        }
    }

    private static @Nullable Order order(Player player, Item item) {
        Orders orders = ORDERS.get(player.getUUID());
        return orders == null ? null : orders.byItem.get(item);
    }

    public static void add(Player player, CraftHandle handle) {
        Orders orders = ORDERS.computeIfAbsent(player.getUUID(), id -> new Orders(player));
        orders.player = player;
        Order order = orders.byItem.computeIfAbsent(handle.item(), Order::new);
        order.handles.removeIf(h -> h.state() == CraftHandle.State.FAILED);
        order.reported.clear();
        order.handles.add(handle);
        if (handle.state() == CraftHandle.State.FAILED) {
            order.reported.add(handle);
        }
        orders.finishedPolls = 0;
    }

    public static List<Order> orders(Player player) {
        Orders orders = ORDERS.get(player.getUUID());
        return orders == null ? List.of() : List.copyOf(orders.byItem.values());
    }

    public static void clear(Player player) {
        if (ORDERS.remove(player.getUUID()) != null) {
            send(player, HudState.NONE);
        }
    }

    public static HudState hud(Player player) {
        Orders orders = ORDERS.get(player.getUUID());
        if (orders == null || orders.byItem.isEmpty()) {
            return HudState.NONE;
        }
        int done = 0;
        int total = 0;
        boolean active = false;
        boolean waiting = false;
        List<Item> inProgress = new ArrayList<>();
        CraftHandle failed = null;
        int failures = 0;
        for (Order order : orders.byItem.values()) {
            done += order.done();
            total += order.requested();
            if (order.active() > 0 && inProgress.size() < HudState.ICONS) {
                inProgress.add(order.item);
            }
            for (CraftHandle handle : order.handles) {
                if (handle.state().active()) {
                    active = true;
                    waiting |= handle.waiting();
                } else if (handle.state() == CraftHandle.State.FAILED) {
                    failed = handle;
                    failures++;
                }
            }
        }
        Component failure = null;
        if (failed != null) {
            MutableComponent line = entry(failed.item(), failed.amount(), failed.failure());
            failure = failures > 1 ? line.append(Component.literal(" +" + (failures - 1))) : line;
        }
        HudState.Phase phase = active ? (waiting ? HudState.Phase.WAITING : HudState.Phase.CRAFTING)
                : failed != null ? HudState.Phase.FAILED : HudState.Phase.READY;
        return new HudState(phase, done, total, List.copyOf(inProgress), failure);
    }

    private static void sendHud(Player player, Orders orders) {
        HudState state = hud(player);
        boolean active = state.phase() == HudState.Phase.CRAFTING || state.phase() == HudState.Phase.WAITING;
        if (!state.equals(orders.sent) || (active && ++orders.sentPolls >= RESEND)) {
            orders.sent = state;
            orders.sentPolls = 0;
            send(player, state);
        }
    }

    private static void send(Player player, HudState state) {
        if (player instanceof ServerPlayer real && !(player instanceof FakePlayer) && real.connection != null) {
            PacketDistributor.sendToPlayer(real, new ToolHudPkt(state));
        }
    }

    public static void update(Player player) {
        Orders orders = ORDERS.get(player.getUUID());
        if (orders == null) {
            return;
        }
        orders.player = player;
        boolean active = false;
        for (Order order : orders.byItem.values()) {
            for (CraftHandle handle : order.handles) {
                handle.update();
                CraftHandle.State state = handle.state();
                active |= state.active();
                if (state == CraftHandle.State.DONE && order.finished.add(handle)) {
                    order.unseen.put(handle, 0);
                } else if (state == CraftHandle.State.DONE) {
                    order.unseen.computeIfPresent(handle, (h, polls) -> polls + 1);
                }
                if (state == CraftHandle.State.FAILED && order.reported.add(handle)) {
                    player.sendOverlayMessage(entry(handle.item(), handle.amount(), handle.failure()).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        orders.finishedPolls = active ? 0 : orders.finishedPolls + 1;
        sendHud(player, orders);
    }

    public static MutableComponent amount(Item item, int count) {
        return Component.empty().append(item.getName(item.getDefaultInstance())).append(Component.literal(" ×" + count));
    }

    public static MutableComponent itemsWord(int count) {
        return Component.translatable(count == 1 ? "message.mm.tool.item" : "message.mm.tool.items");
    }

    public static MutableComponent entry(Item item, int count, @Nullable Component reason) {
        Component why = reason != null ? reason : Component.translatable("message.mm.tool.craft.error");
        return Component.translatable("message.mm.tool.craft.entry", amount(item, count), why);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ORDERS.isEmpty() || event.getServer().getTickCount() % INTERVAL != 0) {
            return;
        }
        var it = ORDERS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Orders orders = entry.getValue();
            ServerPlayer listed = event.getServer().getPlayerList().getPlayer(entry.getKey());
            Player player = listed != null ? listed : orders.player instanceof FakePlayer fake && !fake.isRemoved() ? fake : null;
            if (player == null || orders.finishedPolls > KEEP_FINISHED) {
                it.remove();
                if (player != null) {
                    send(player, HudState.NONE);
                }
                continue;
            }
            update(player);
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
