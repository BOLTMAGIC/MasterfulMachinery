package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Admin tool screen -> server: one validated change to which blocks a helper may stand in for. */
public record HelperSlotEditPkt(HelperSlotOverrides.Action action, ResourceLocation structure, char key, int value) {
    public static void encode(HelperSlotEditPkt packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeResourceLocation(packet.structure);
        buf.writeChar(packet.key);
        buf.writeInt(packet.value);
    }

    public static HelperSlotEditPkt decode(FriendlyByteBuf buf) {
        return new HelperSlotEditPkt(buf.readEnum(HelperSlotOverrides.Action.class), buf.readResourceLocation(),
                buf.readChar(), buf.readInt());
    }

    public static void handle(HelperSlotEditPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.hasPermissions(2) || !(sender.containerMenu instanceof MultiblockToolMenu)) return;
            HelperSlotOverrides overrides = HelperSlotOverrides.get(sender.getServer());
            if (overrides.apply(packet.action, packet.structure, packet.key, packet.value)) {
                MMNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new HelperSlotsSyncPkt(overrides.snapshot()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
