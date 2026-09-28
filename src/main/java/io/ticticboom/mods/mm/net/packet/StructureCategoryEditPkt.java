package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Admin tool screen -> server: one validated category change. */
public record StructureCategoryEditPkt(StructureCategories.Action action, String target, String value) {
    public static void encode(StructureCategoryEditPkt packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.target, 256);
        buf.writeUtf(packet.value, 32);
    }

    public static StructureCategoryEditPkt decode(FriendlyByteBuf buf) {
        return new StructureCategoryEditPkt(buf.readEnum(StructureCategories.Action.class), buf.readUtf(256), buf.readUtf(32));
    }

    public static void handle(StructureCategoryEditPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.hasPermissions(2) || !(sender.containerMenu instanceof MultiblockToolMenu)) return;
            StructureCategories categories = StructureCategories.get(sender.getServer());
            if (categories.apply(packet.action, packet.target, packet.value)) {
                MMNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new StructureCategoriesSyncPkt(categories.snapshot()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
