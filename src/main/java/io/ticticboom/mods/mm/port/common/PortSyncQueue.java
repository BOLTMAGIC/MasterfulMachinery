package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Ref.ID)
public final class PortSyncQueue {

    private static final List<AbstractPortBlockEntity> PENDING = new ArrayList<>();

    private PortSyncQueue() {
    }

    static void add(AbstractPortBlockEntity port) {
        PENDING.add(port);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            PENDING.removeIf(AbstractPortBlockEntity::flushSync);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
