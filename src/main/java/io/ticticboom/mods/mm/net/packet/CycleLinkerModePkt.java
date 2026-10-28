package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.networklink.LinkerMode;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleLinkerModePkt(int direction) implements CustomPacketPayload {

    public static final Type<CycleLinkerModePkt> TYPE = new Type<>(Ref.id("cycle_linker_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleLinkerModePkt> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(CycleLinkerModePkt::new, CycleLinkerModePkt::direction);

    public static void handle(CycleLinkerModePkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var stack = player.getMainHandItem();
            if (!NetworkLink.isLinker(stack)) return;
            var mode = LinkerMode.get(stack).cycle(Integer.signum(pkt.direction));
            mode.set(stack);
            player.sendOverlayMessage(mode.displayName());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
