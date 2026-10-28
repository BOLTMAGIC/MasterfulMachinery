package io.ticticboom.mods.mm.compat.ae2.linker;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.LinkerMode;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

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
                showOrRemoveLink((ServerLevel) level, player, context);
            }
            return InteractionResult.SUCCESS;
        }
        if (be instanceof MachineControllerBlockEntity controller) {
            if (!level.isClientSide()) {
                useOnController(player, context.getItemInHand(), controller);
            }
            return InteractionResult.SUCCESS;
        }
        if (be instanceof IInWorldGridNodeHost host) {
            if (!level.isClientSide()) {
                rememberNetwork(level, player, context, host);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || LinkerMode.get(stack) != LinkerMode.INFO || getNetwork(stack) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            setNetwork(stack, null);
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.network_cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    private static void showOrRemoveLink(ServerLevel level, Player player, UseOnContext context) {
        MachineControllerBlockEntity controller = NetworkLink.controllerAt(level, context.getClickedPos());
        LinkData link = controller == null ? null : controller.getNetworkLink();
        if (link == null) {
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.info.not_linked").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!Permissions.canAccess(player, link.owner())) {
            notOwner(player, link);
            return;
        }
        if (player.isShiftKeyDown()) {
            controller.setNetworkLink(null);
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.unlinked"));
            return;
        }
        LinkData.NetworkPos network = link.network();
        boolean online = NetworkAccess.storage(level.getServer(), network) != null;
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.info.linked",
                        link.ownerName(), network.pos().toShortString(), network.dimension().identifier().getPath())
                .append(" ")
                .append(Component.translatable(online ? "message.mm.network_linker.info.online" : "message.mm.network_linker.info.offline")
                        .withStyle(online ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("message.mm.network_linker.info.unlink_hint").withStyle(ChatFormatting.GRAY)));
    }

    private static void rememberNetwork(Level level, Player player, UseOnContext context, IInWorldGridNodeHost host) {
        var network = NetworkAccess.clicked(level, context.getClickedPos(), context.getClickedFace(), host);
        if (network == null) {
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.not_a_network").withStyle(ChatFormatting.RED));
            return;
        }
        setNetwork(context.getItemInHand(), network);
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.network_saved", context.getClickedPos().toShortString()));
    }

    private static void useOnController(Player player, ItemStack stack, MachineControllerBlockEntity controller) {
        LinkData existing = controller.getNetworkLink();
        if (existing != null && !Permissions.canAccess(player, existing.owner())) {
            notOwner(player, existing);
            return;
        }
        if (player.isShiftKeyDown()) {
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.use_info_mode").withStyle(ChatFormatting.GRAY));
            return;
        }
        LinkData.NetworkPos network = getNetwork(stack);
        if (network == null) {
            player.sendOverlayMessage(Component.translatable("message.mm.network_linker.no_network").withStyle(ChatFormatting.RED));
            return;
        }
        var link = existing != null
                ? new LinkData(existing.owner(), existing.ownerName(), network)
                : new LinkData(player.getUUID(), player.getGameProfile().name(), network);
        controller.setNetworkLink(link);
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.linked", link.ownerName(), network.pos().toShortString()));
    }

    private static void notOwner(Player player, LinkData link) {
        player.sendOverlayMessage(Component.translatable("message.mm.network_linker.not_owner", link.ownerName()).withStyle(ChatFormatting.RED));
    }

    @Nullable
    public static LinkData.NetworkPos getNetwork(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        return tag.contains(NETWORK_TAG) ? LinkData.NetworkPos.load(tag.getCompoundOrEmpty(NETWORK_TAG)) : null;
    }

    public static void setNetwork(ItemStack stack, @Nullable LinkData.NetworkPos network) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (network == null) {
                tag.remove(NETWORK_TAG);
            } else {
                tag.put(NETWORK_TAG, network.save());
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().append(" (").append(LinkerMode.get(stack).displayName()).append(")");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        LinkData.NetworkPos network = getNetwork(stack);
        if (network == null) {
            tooltip.accept(Component.translatable("tooltip.mm.network_linker.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.mm.network_linker.network",
                    network.pos().toShortString(), network.dimension().identifier().toString()).withStyle(ChatFormatting.AQUA));
        }
        var mode = LinkerMode.get(stack);
        String usageKey = "tooltip.mm.network_linker.usage." + mode.name().toLowerCase(Locale.ROOT);
        for (int line = 1; line <= (mode == LinkerMode.INFO ? 3 : 2); line++) {
            tooltip.accept(Component.translatable(usageKey + "." + line).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.mm.network_linker.mode_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getNetwork(stack) != null;
    }
}
