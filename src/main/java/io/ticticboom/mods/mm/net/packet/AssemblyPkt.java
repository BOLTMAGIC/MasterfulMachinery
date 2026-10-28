package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record AssemblyPkt(BlockPos pos, Action action, String key, int value) implements CustomPacketPayload {
    public enum Action { SET_TIER, SET_STRUCTURE, START }

    public static final Type<AssemblyPkt> TYPE = new Type<>(Ref.id("assembly"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeEnum(pkt.action);
                buf.writeUtf(pkt.key, 256);
                buf.writeVarInt(pkt.value);
            },
            buf -> new AssemblyPkt(buf.readBlockPos(), buf.readEnum(Action.class), buf.readUtf(256), buf.readVarInt()));

    public static void handle(AssemblyPkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            if (!(sender.containerMenu instanceof MachineControllerMenu menu) || !menu.stillValid(sender)) return;
            if (!(menu.getBe().getBlockEntity() instanceof MachineControllerBlockEntity controller) || !controller.getBlockPos().equals(pkt.pos)) return;
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(sender, link.owner())) return;
            switch (pkt.action) {
                case SET_TIER -> controller.setAssemblyTier(pkt.key, pkt.value);
                case SET_STRUCTURE -> {
                    Identifier id = Identifier.tryParse(pkt.key);
                    if (id != null) {
                        controller.setAssemblyStructureId(id);
                    }
                }
                case START -> start(sender, controller, Identifier.tryParse(pkt.key));
            }
        });
    }

    private static void start(ServerPlayer player, MachineControllerBlockEntity controller, @Nullable Identifier shownId) {
        StructureModel structure = controller.findAssemblyCandidate(shownId);
        if (structure == null) {
            structure = controller.getAssemblyStructure();
        }
        if (structure == null) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.no_structure"), true);
            return;
        }
        BlockPos pos = controller.getBlockPos();
        var plan = AssemblyPlanner.planCompletion(player.level(), structure, pos, controller.getAssemblyTiers(), b -> PlayerMaterials.has(player, b));
        if (plan == null) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.already"), true);
            return;
        }
        if (!AssemblyJobs.start(player, pos, plan)) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.busy"), true);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
