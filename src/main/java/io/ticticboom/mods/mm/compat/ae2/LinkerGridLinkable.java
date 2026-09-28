package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.features.IGridLinkableHandler;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * Lets both MM network-aware items remember a Wireless Access Point's grid.
 */
public class LinkerGridLinkable implements IGridLinkableHandler {

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof LinkerItem || stack.getItem() instanceof MultiblockToolItem;
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        // the access point's node isn't sided, NetworkAccess tries every face
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
