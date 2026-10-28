package io.ticticboom.mods.mm.item;

import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WrenchItem extends Item {
    private static final PortSides.Relative[] SUMMARY_ORDER = {
            PortSides.Relative.TOP, PortSides.Relative.BOTTOM, PortSides.Relative.FRONT,
            PortSides.Relative.BACK, PortSides.Relative.LEFT, PortSides.Relative.RIGHT};
    private static final Direction[] SUMMARY_COMPASS = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public WrenchItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof AbstractPortBlockEntity port)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            configurePort(player, context, port);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void configurePort(Player player, UseOnContext context, AbstractPortBlockEntity port) {
        if (!WrenchAccess.canConfigure(player, port)) {
            return;
        }
        PortAutoIO autoIO = port.getAutoIO();
        if (autoIO == null) {
            player.sendOverlayMessage(Component.translatable("message.mm.wrench.no_autoio").withStyle(ChatFormatting.RED));
            return;
        }
        if (player.isShiftKeyDown()) {
            player.sendOverlayMessage(summary(autoIO, port.getMachineFront()));
            return;
        }
        Direction side = context.getClickedFace();
        autoIO.toggleSide(side);
        port.setChanged();
        player.sendOverlayMessage(Component.translatable(PortSides.nameKey(side, port.getMachineFront()))
                .append(": ").append(stateName(autoIO, side)));
    }

    private static Component summary(PortAutoIO autoIO, @Nullable Direction front) {
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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mm.wrench.usage.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mm.wrench.usage.2").withStyle(ChatFormatting.GRAY));
    }
}
