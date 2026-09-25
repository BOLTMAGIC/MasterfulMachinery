package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.LinkerMode;
import io.ticticboom.mods.mm.networklink.MultiblockLookup;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Machine Network Linker. Sneak + mouse wheel switches between two modes:
 * <ul>
 *     <li>Linking: save an AE2 network (Wireless Access Point slot, or right-click an AE2 block),
 *     then right-click a controller to link it to yourself and that network.</li>
 *     <li>Info: right-click a controller or port to see what the machine is linked to;
 *     sneak + right-click it to remove the link, sneak + right-click the air to forget the saved network.</li>
 * </ul>
 * Port sides are configured with the Machine Wrench.
 */
public class LinkerItem extends Item {
    private static final String NETWORK_TAG = "Network";

    public LinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (LinkerMode.get(context.getItemInHand()) == LinkerMode.INFO) {
            if (!(be instanceof MachineControllerBlockEntity) && !(be instanceof IPortBlockEntity)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                showOrRemoveLink((ServerLevel) level, player, context, be);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (be instanceof MachineControllerBlockEntity controller) {
            if (!level.isClientSide()) {
                useOnController(player, context.getItemInHand(), controller);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (be instanceof IInWorldGridNodeHost host) {
            if (!level.isClientSide()) {
                rememberNetwork(level, player, context, host);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || LinkerMode.get(stack) != LinkerMode.INFO || getNetwork(stack) == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            setNetwork(stack, null);
            player.displayClientMessage(Component.translatable("message.mm.network_linker.network_cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void showOrRemoveLink(ServerLevel level, Player player, UseOnContext context, BlockEntity be) {
        MachineControllerBlockEntity controller = be instanceof MachineControllerBlockEntity c
                ? c
                : MultiblockLookup.findLinkedController(level, context.getClickedPos(), ((IPortBlockEntity) be).getStorage());
        LinkData link = controller == null ? null : controller.getNetworkLink();
        if (link == null) {
            player.displayClientMessage(Component.translatable("message.mm.network_linker.info.not_linked").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!Permissions.canAccess(player, link.owner())) {
            notOwner(player, link);
            return;
        }
        if (player.isShiftKeyDown()) {
            controller.setNetworkLink(null);
            player.displayClientMessage(Component.translatable("message.mm.network_linker.unlinked"), true);
            return;
        }
        LinkData.NetworkPos network = link.network();
        boolean online = NetworkAccess.storage(level.getServer(), network) != null;
        player.displayClientMessage(Component.translatable("message.mm.network_linker.info.linked",
                        link.ownerName(), network.pos().toShortString(), network.dimension().location().getPath())
                .append(" ")
                .append(Component.translatable(online ? "message.mm.network_linker.info.online" : "message.mm.network_linker.info.offline")
                        .withStyle(online ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("message.mm.network_linker.info.unlink_hint").withStyle(ChatFormatting.GRAY)), true);
    }

    private static void rememberNetwork(Level level, Player player, UseOnContext context, IInWorldGridNodeHost host) {
        var face = context.getClickedFace();
        if (NetworkAccess.nodeOf(host, face) == null) {
            player.displayClientMessage(Component.translatable("message.mm.network_linker.not_a_network").withStyle(ChatFormatting.RED), true);
            return;
        }
        setNetwork(context.getItemInHand(), new LinkData.NetworkPos(level.dimension(), context.getClickedPos(), face));
        player.displayClientMessage(Component.translatable("message.mm.network_linker.network_saved", context.getClickedPos().toShortString()), true);
    }

    private static void useOnController(Player player, ItemStack stack, MachineControllerBlockEntity controller) {
        LinkData existing = controller.getNetworkLink();
        if (existing != null && !Permissions.canAccess(player, existing.owner())) {
            notOwner(player, existing);
            return;
        }

        if (player.isShiftKeyDown()) {
            // unlinking moved to Info mode, so a slip while linking can't drop the link
            player.displayClientMessage(Component.translatable("message.mm.network_linker.use_info_mode").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        LinkData.NetworkPos network = getNetwork(stack);
        if (network == null) {
            player.displayClientMessage(Component.translatable("message.mm.network_linker.no_network").withStyle(ChatFormatting.RED), true);
            return;
        }
        // re-linking keeps the original owner; only unlinked machines are claimed by the clicking player
        var link = existing != null
                ? new LinkData(existing.owner(), existing.ownerName(), network)
                : new LinkData(player.getUUID(), player.getGameProfile().getName(), network);
        controller.setNetworkLink(link);
        player.displayClientMessage(Component.translatable("message.mm.network_linker.linked", link.ownerName(), network.pos().toShortString()), true);
    }

    static void notOwner(Player player, LinkData link) {
        player.displayClientMessage(Component.translatable("message.mm.network_linker.not_owner", link.ownerName()).withStyle(ChatFormatting.RED), true);
    }

    @Nullable
    public static LinkData.NetworkPos getNetwork(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NETWORK_TAG)) {
            return null;
        }
        return LinkData.NetworkPos.load(tag.getCompound(NETWORK_TAG));
    }

    public static void setNetwork(ItemStack stack, @Nullable LinkData.NetworkPos network) {
        if (network == null) {
            var tag = stack.getTag();
            if (tag != null) {
                tag.remove(NETWORK_TAG);
            }
        } else {
            stack.getOrCreateTag().put(NETWORK_TAG, network.save());
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().append(" (").append(LinkerMode.get(stack).displayName()).append(")");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        LinkData.NetworkPos network = getNetwork(stack);
        if (network == null) {
            tooltip.add(Component.translatable("tooltip.mm.network_linker.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mm.network_linker.network",
                    network.pos().toShortString(), network.dimension().location().toString()).withStyle(ChatFormatting.AQUA));
        }
        var mode = LinkerMode.get(stack);
        tooltip.add(Component.translatable("tooltip.mm.network_linker.usage." + mode.name().toLowerCase()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.mm.network_linker.mode_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getNetwork(stack) != null;
    }
}
