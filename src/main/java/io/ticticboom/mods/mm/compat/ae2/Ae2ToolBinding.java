package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.UseOnContext;

/** Shift+right-click with the Multiblock Tool on an AE2 block binds the tool to that block's network. */
public final class Ae2ToolBinding {
    private Ae2ToolBinding() {
    }

    /** @return true when the clicked block is an AE2 grid host (bound, or told why not); false to let the tool go on */
    public static boolean tryBind(ServerPlayer player, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IInWorldGridNodeHost host)) {
            return false;
        }
        var network = NetworkAccess.clicked(context.getLevel(), context.getClickedPos(), context.getClickedFace(), host);
        if (network == null) {
            player.displayClientMessage(Component.translatable("message.mm.network_linker.not_a_network").withStyle(ChatFormatting.RED), true);
            return true;
        }
        ToolData.setNetwork(context.getItemInHand(), network);
        player.displayClientMessage(Component.translatable("message.mm.tool.me_bound",
                network.pos().toShortString(), network.dimension().location().getPath()), true);
        return true;
    }
}