package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.networklink.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Controller screen -> server: change the controller's recipe order or its name.
 */
public record ControllerSettingsPkt(BlockPos pos, Setting setting, String value) {
    public enum Setting { RECIPE_ORDER, NAME }

    private static final double MAX_DISTANCE_SQR = 64 * 64;

    public static void encode(ControllerSettingsPkt pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeEnum(pkt.setting);
        buf.writeUtf(pkt.value, MachineControllerBlockEntity.MAX_NAME_LENGTH * 4);
    }

    public static ControllerSettingsPkt decode(FriendlyByteBuf buf) {
        return new ControllerSettingsPkt(buf.readBlockPos(), buf.readEnum(Setting.class), buf.readUtf(MachineControllerBlockEntity.MAX_NAME_LENGTH * 4));
    }

    public static void handle(ControllerSettingsPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.level().isLoaded(pkt.pos)
                    || sender.distanceToSqr(pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5) > MAX_DISTANCE_SQR) {
                return;
            }
            if (!(sender.level().getBlockEntity(pkt.pos) instanceof MachineControllerBlockEntity controller)) {
                return;
            }
            // a machine linked to a network belongs to its owner's team
            var link = controller.getNetworkLink();
            if (link != null && !Permissions.canAccess(sender, link.owner())) {
                return;
            }
            switch (pkt.setting) {
                case RECIPE_ORDER -> controller.setRecipeSelectionMode(RecipeSelectionMode.parse(pkt.value));
                case NAME -> controller.setCustomName(pkt.value);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
