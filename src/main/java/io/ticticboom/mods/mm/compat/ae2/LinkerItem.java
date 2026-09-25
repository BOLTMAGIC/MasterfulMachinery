package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.LinkerMode;
import io.ticticboom.mods.mm.networklink.MultiblockLookup;
import io.ticticboom.mods.mm.networklink.Permissions;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
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
 *     then right-click a controller to link it to yourself and that network; sneak + right-click
 *     a controller to unlink it.</li>
 *     <li>Configure: right-click a port side to toggle its auto I/O; sneak + right-click shows all sides.</li>
 * </ul>
 */
public class LinkerItem extends Item {
    private static final String NETWORK_TAG = "Network";
    private static final PortSides.Relative[] SUMMARY_ORDER = {
            PortSides.Relative.TOP, PortSides.Relative.BOTTOM, PortSides.Relative.FRONT,
            PortSides.Relative.BACK, PortSides.Relative.LEFT, PortSides.Relative.RIGHT};
    private static final Direction[] SUMMARY_COMPASS = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

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
        if (LinkerMode.get(context.getItemInHand()) == LinkerMode.CONFIGURE) {
            if (!(be instanceof AbstractPortBlockEntity port)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                configurePort((ServerLevel) level, player, context, port);
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

    private static void configurePort(ServerLevel level, Player player, UseOnContext context, AbstractPortBlockEntity port) {
        var owner = MultiblockLookup.findLinkedController(level, context.getClickedPos(), port.getStorage());
        if (owner != null && !Permissions.canAccess(player, owner.getNetworkLink().owner())) {
            notOwner(player, owner.getNetworkLink());
            return;
        }
        PortAutoIO autoIO = port.getAutoIO();
        if (autoIO == null) {
            player.displayClientMessage(Component.translatable("message.mm.network_linker.no_autoio").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean frontChanged = autoIO.refreshFront(level);
        if (player.isShiftKeyDown()) {
            player.displayClientMessage(summary(autoIO), true);
            if (frontChanged) {
                port.setChanged();
            }
            return;
        }
        Direction side = context.getClickedFace();
        autoIO.toggleSide(side);
        port.setChanged();
        player.displayClientMessage(Component.translatable(PortSides.nameKey(side, autoIO.getFront()))
                .append(": ").append(stateName(autoIO, side)), true);
    }

    private static Component summary(PortAutoIO autoIO) {
        Direction front = autoIO.getFront();
        MutableComponent line = Component.empty();
        for (int i = 0; i < SUMMARY_ORDER.length; i++) {
            Direction side = front == null ? SUMMARY_COMPASS[i] : PortSides.toWorld(SUMMARY_ORDER[i], front);
            if (i > 0) {
                line.append("  ");
            }
            line.append(Component.translatable(PortSides.nameKey(side, front))
                    .withStyle(autoIO.isSideEnabled(side) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        return line.append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(autoIO.isPull() ? "gui.mm.port.side.state.pull" : "gui.mm.port.side.state.push"));
    }

    private static Component stateName(PortAutoIO autoIO, Direction side) {
        if (!autoIO.isSideEnabled(side)) {
            return Component.translatable("gui.mm.port.side.state.off").withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable(autoIO.isPull() ? "gui.mm.port.side.state.pull" : "gui.mm.port.side.state.push")
                .withStyle(ChatFormatting.GREEN);
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
            if (existing != null) {
                controller.setNetworkLink(null);
                player.displayClientMessage(Component.translatable("message.mm.network_linker.unlinked"), true);
            }
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
