package io.ticticboom.mods.mm.debug.tool;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.debug.output.CollectedDebugData;
import io.ticticboom.mods.mm.debug.output.DebugOutputManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class DebugToolItem extends Item {
    public DebugToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!MMConfig.DEBUG_TOOL || ctx.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!ctx.getItemInHand().getItem().equals(this)) {
            return InteractionResult.PASS;
        }
        var block = ctx.getLevel().getBlockState(ctx.getClickedPos());

        if (!(block.getBlock() instanceof MachineControllerBlock)) {
            return InteractionResult.PASS;
        }

        CollectedDebugData collect = DebugOutputManager.collect(ctx.getLevel(), ctx.getClickedPos());
        var path = DebugOutputManager.output(collect);
        var msg = Component.translatable("item.mm.debug_tool.message.saved_to", Component.literal(path).withStyle(ChatFormatting.DARK_AQUA))
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(path)))
                .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.translatable("item.mm.debug_tool.message.copy_hint"))));
        ctx.getPlayer().sendSystemMessage(Component.translatable("item.mm.debug_tool.message.saved").withStyle(ChatFormatting.GREEN));
        ctx.getPlayer().sendSystemMessage(msg);

        return InteractionResult.SUCCESS;
    }
}
