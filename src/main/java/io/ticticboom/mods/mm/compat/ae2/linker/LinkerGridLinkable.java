package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.features.IGridLinkableHandler;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

public class LinkerGridLinkable implements IGridLinkableHandler {

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof LinkerItem || stack.getItem() instanceof MultiblockToolItem;
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        var network = new LinkData.NetworkPos(pos.dimension(), pos.pos(), null);
        if (stack.getItem() instanceof MultiblockToolItem) ToolData.setNetwork(stack, network);
        else LinkerItem.setNetwork(stack, network);
    }

    @Override
    public void unlink(ItemStack stack) {
        if (stack.getItem() instanceof MultiblockToolItem) ToolData.setNetwork(stack, null);
        else LinkerItem.setNetwork(stack, null);
    }
}
