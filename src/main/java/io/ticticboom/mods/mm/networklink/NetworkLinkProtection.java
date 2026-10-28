package io.ticticboom.mods.mm.networklink;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class NetworkLinkProtection {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (NetworkLink.isLinker(player.getMainHandItem()) || NetworkLink.isLinker(player.getOffhandItem())) {
            var clicked = event.getLevel().getBlockEntity(event.getPos());
            if (clicked instanceof MachineControllerBlockEntity || clicked instanceof IPortBlockEntity
                    || clicked instanceof appeng.api.networking.IInWorldGridNodeHost) {
                event.setUseBlock(TriState.FALSE);
            }
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        LinkData link = NetworkLink.linkAt(event.getLevel(), event.getPos());
        if (link != null && !Permissions.canAccess(player, link.owner())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            denied(player, link);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof net.minecraft.world.level.Level level)) {
            return;
        }
        LinkData link = NetworkLink.linkAt(level, event.getPos());
        if (link != null && !Permissions.canAccess(event.getPlayer(), link.owner())) {
            event.setCanceled(true);
            denied(event.getPlayer(), link);
        }
    }

    static void denied(Player player, LinkData link) {
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.not_owner", link.ownerName()).withStyle(ChatFormatting.RED));
    }
}
