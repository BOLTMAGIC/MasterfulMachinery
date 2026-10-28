package io.ticticboom.mods.mm.compat.ae2;

import io.ticticboom.mods.mm.compat.ae2.linker.NetworkAccess;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.UseOnContext;

public final class Ae2ToolBinding {
    private Ae2ToolBinding() {
    }

    public static boolean tryBind(ServerPlayer player, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IInWorldGridNodeHost host)) {
            return false;
        }
        var network = NetworkAccess.clicked(context.getLevel(), context.getClickedPos(), context.getClickedFace(), host);
        if (network == null) {
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.not_a_network").withStyle(ChatFormatting.RED));
            return true;
        }
        IGridNode node = NetworkAccess.nodeOf(host, context.getClickedFace());
        IGrid grid = node == null ? null : node.getGrid();
        boolean allowed = grid != null && (NetworkAccess.owners(grid).isEmpty()
                ? context.getLevel().mayInteract(player, context.getClickedPos())
                : NetworkAccess.mayUse(player, grid));
        if (!allowed) {
            player.sendOverlayMessage(Component.translatable("message.mm.tool.me_no_access").withStyle(ChatFormatting.RED));
            return true;
        }
        ToolData.setNetwork(context.getItemInHand(), network);
        player.sendOverlayMessage(Component.translatable("message.mm.tool.me_bound",
                network.pos().toShortString(), network.dimension().identifier().getPath()));
        return true;
    }
}
