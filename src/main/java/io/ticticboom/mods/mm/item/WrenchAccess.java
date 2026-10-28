package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

final class WrenchAccess {

    private WrenchAccess() {
    }

    static boolean canConfigure(Player player, AbstractPortBlockEntity port) {
        var link = NetworkLink.linkAt(port.getLevel(), port.getBlockPos());
        if (link == null || Permissions.canAccess(player, link.owner())) {
            return true;
        }
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.not_owner", link.ownerName())
                .withStyle(ChatFormatting.RED));
        return false;
    }
}
