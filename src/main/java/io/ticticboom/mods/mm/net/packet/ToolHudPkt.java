package io.ticticboom.mods.mm.net.packet;

import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.me.HudState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ToolHudPkt(HudState state) implements CustomPacketPayload {
    public static final Type<ToolHudPkt> TYPE = new Type<>(Ref.id("tool_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolHudPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ToolHudPkt::decode);

    @Nullable
    private static Consumer<HudState> clientHandler;

    public static void setClientHandler(Consumer<HudState> handler) {
        clientHandler = handler;
    }

    public static void encode(ToolHudPkt pkt, RegistryFriendlyByteBuf buf) {
        HudState state = pkt.state;
        buf.writeEnum(state.phase());
        buf.writeVarInt(state.done());
        buf.writeVarInt(state.total());
        List<Item> items = state.inProgress().subList(0, Math.min(HudState.ICONS, state.inProgress().size()));
        buf.writeVarInt(items.size());
        for (Item item : items) {
            buf.writeIdentifier(BuiltInRegistries.ITEM.getKey(item));
        }
        buf.writeBoolean(state.failure() != null);
        if (state.failure() != null) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, state.failure());
        }
    }

    public static ToolHudPkt decode(RegistryFriendlyByteBuf buf) {
        HudState.Phase phase = buf.readEnum(HudState.Phase.class);
        int done = buf.readVarInt();
        int total = buf.readVarInt();
        int count = buf.readVarInt();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR && items.size() < HudState.ICONS) {
                items.add(item);
            }
        }
        Component failure = buf.readBoolean() ? ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf) : null;
        return new ToolHudPkt(new HudState(phase, done, total, List.copyOf(items), failure));
    }

    public static void handle(ToolHudPkt pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (clientHandler != null) {
                clientHandler.accept(pkt.state);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
