package io.ticticboom.mods.mm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * A block item showing the block's numbered tooltip lines (block.mm.&lt;id&gt;.tooltip.1 ..). Blocks no longer add
 * tooltip lines themselves since 1.21.5.
 */
public class TooltipBlockItem extends BlockItem {
    private final int lines;

    public TooltipBlockItem(Block block, Properties properties, int lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= lines; line++) {
            tooltip.accept(Component.translatable(getBlock().getDescriptionId() + ".tooltip." + line).withStyle(ChatFormatting.GRAY));
        }
    }
}
