package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Controller Assemble screen -> server: choose a tier or structure, or start building. START carries the
 * structure id the screen shows in {@code key}.
 */
public record AssemblyPkt(BlockPos pos, Action action, String key, int value) {
    public enum Action { SET_TIER, SET_STRUCTURE, START }

    private static final double MAX_DISTANCE_SQR = 64 * 64;

    public static void encode(AssemblyPkt pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeEnum(pkt.action);
        buf.writeUtf(pkt.key, 256);
        buf.writeVarInt(pkt.value);
    }

    public static AssemblyPkt decode(FriendlyByteBuf buf) {
        return new AssemblyPkt(buf.readBlockPos(), buf.readEnum(Action.class), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(AssemblyPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.level().isLoaded(pkt.pos)
                    || sender.distanceToSqr(pkt.pos.getCenter()) > MAX_DISTANCE_SQR) {
                return;
            }
            if (!(sender.level().getBlockEntity(pkt.pos) instanceof MachineControllerBlockEntity controller)) {
                return;
            }
            // only from the controller's own screen, which the server keeps open while Assemble is shown
            if (!(sender.containerMenu instanceof MachineControllerMenu menu) || menu.getBe() != controller) {
                return;
            }
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(sender, link.owner())) {
                return;
            }
            switch (pkt.action) {
                case SET_TIER -> controller.setAssemblyTier(pkt.key, pkt.value);
                case SET_STRUCTURE -> {
                    ResourceLocation id = ResourceLocation.tryParse(pkt.key);
                    if (id != null) {
                        controller.setAssemblyStructureId(id);
                    }
                }
                case START -> start(sender, controller, ResourceLocation.tryParse(pkt.key));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void start(ServerPlayer player, MachineControllerBlockEntity controller, @Nullable ResourceLocation shownId) {
        // build what the screen showed; fall back to the stored choice if that is not a candidate
        StructureModel structure = controller.findAssemblyCandidate(shownId);
        if (structure == null) {
            structure = controller.getAssemblyStructure();
        }
        if (structure == null) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.no_structure"), true);
            return;
        }
        BlockPos pos = controller.getBlockPos();
        if (structure.formed(player.level(), pos)) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.already"), true);
            return;
        }
        var facing = controller.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var rotation = AssemblyPlanner.bestRotation(player.level(), structure, pos, facing);
        var plan = AssemblyPlanner.plan(structure, pos, rotation, controller.getAssemblyTiers(), b -> PlayerMaterials.has(player, b));
        if (!AssemblyJobs.start(player, pos, plan)) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.busy"), true);
        }
    }
}
