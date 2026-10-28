package io.ticticboom.mods.mm.net.packet;

import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;


public record StructureCategoryEditPkt(StructureCategories.Action action, String target, String value) implements CustomPacketPayload {
    public static final Type<StructureCategoryEditPkt> TYPE = new Type<>(Ref.id("structure_category_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureCategoryEditPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), StructureCategoryEditPkt::decode);

    public static void encode(StructureCategoryEditPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.target, 256);
        buf.writeUtf(packet.value, 32);
    }

    public static StructureCategoryEditPkt decode(RegistryFriendlyByteBuf buf) {
        return new StructureCategoryEditPkt(buf.readEnum(StructureCategories.Action.class), buf.readUtf(256), buf.readUtf(32));
    }

    public static void handle(StructureCategoryEditPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.player() instanceof ServerPlayer sp ? sp : null;
            if (sender == null || !sender.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || !(sender.containerMenu instanceof MultiblockToolMenu)) return;
            StructureCategories categories = StructureCategories.get(sender.level().getServer());
            if (categories.apply(packet.action, packet.target, packet.value)) {
                PacketDistributor.sendToAllPlayers(new StructureCategoriesSyncPkt(categories.snapshot()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
