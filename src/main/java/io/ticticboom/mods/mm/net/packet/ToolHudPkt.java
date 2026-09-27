package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.builder.me.HudState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Server -> client: the Multiblock Tool HUD's craft progress ({@link HudState}), sent when it changes and every
 * 20 ticks while crafts are on their way. {@link HudState.Phase#NONE} hides the HUD.
 */
public record ToolHudPkt(HudState state) {
    /** Set by the client setup; the packet itself never touches client classes. */
    @Nullable
    private static Consumer<HudState> clientHandler;

    public static void setClientHandler(Consumer<HudState> handler) {
        clientHandler = handler;
    }

    public static void encode(ToolHudPkt pkt, FriendlyByteBuf buf) {
        HudState state = pkt.state;
        buf.writeEnum(state.phase());
        buf.writeVarInt(state.done());
        buf.writeVarInt(state.total());
        List<Item> items = state.inProgress().subList(0, Math.min(HudState.ICONS, state.inProgress().size()));
        buf.writeVarInt(items.size());
        for (Item item : items) {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
        }
        buf.writeBoolean(state.failure() != null);
        if (state.failure() != null) {
            buf.writeComponent(state.failure());
        }
    }

    public static ToolHudPkt decode(FriendlyByteBuf buf) {
        HudState.Phase phase = buf.readEnum(HudState.Phase.class);
        int done = buf.readVarInt();
        int total = buf.readVarInt();
        int count = buf.readVarInt();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR && items.size() < HudState.ICONS) {
                items.add(item);
            }
        }
        Component failure = buf.readBoolean() ? buf.readComponent() : null;
        return new ToolHudPkt(new HudState(phase, done, total, List.copyOf(items), failure));
    }

    public static void handle(ToolHudPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (clientHandler != null) {
                clientHandler.accept(pkt.state);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
