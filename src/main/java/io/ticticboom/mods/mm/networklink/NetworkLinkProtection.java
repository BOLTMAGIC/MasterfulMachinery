package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Only the owner, their FTB team and operators may open or break a linked machine.
 */
public final class NetworkLinkProtection {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (NetworkLink.isLinker(player.getMainHandItem()) || NetworkLink.isLinker(player.getOffhandItem())) {
            // Only deny block use for targets that would consume the click (open a GUI) before the item runs.
            var clickedBe = event.getLevel().getBlockEntity(event.getPos());
            if (clickedBe instanceof MachineControllerBlockEntity
                    || clickedBe instanceof IPortBlockEntity
                    || clickedBe instanceof appeng.api.networking.IInWorldGridNodeHost) {
                event.setUseBlock(Event.Result.DENY);
            }
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LinkData link = linkAt(level, event.getPos());
        if (link != null && !Permissions.canAccess(player, link.owner())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            denied(player, link);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LinkData link = linkAt(level, event.getPos());
        if (link != null && !Permissions.canAccess(event.getPlayer(), link.owner())) {
            event.setCanceled(true);
            denied(event.getPlayer(), link);
        }
    }

    /**
     * @return the link protecting the block at pos: a linked controller, or a port of a linked multiblock
     */
    @Nullable
    private static LinkData linkAt(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof MachineControllerBlockEntity controller) {
            return controller.getNetworkLink();
        }
        if (be instanceof IPortBlockEntity port) {
            var controller = MultiblockLookup.findLinkedController(level, pos, port.getStorage());
            return controller == null ? null : controller.getNetworkLink();
        }
        return null;
    }

    private static void denied(Player player, LinkData link) {
        player.displayClientMessage(Component.translatable("message.mm.network_linker.not_owner", link.ownerName()).withStyle(ChatFormatting.RED), true);
    }
}
