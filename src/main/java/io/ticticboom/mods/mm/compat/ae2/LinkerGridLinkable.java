package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.features.IGridLinkableHandler;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * Lets the linker be linked in a Wireless Access Point's slot, like AE2's wireless terminals.
 */
public class LinkerGridLinkable implements IGridLinkableHandler {

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof LinkerItem;
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        // the access point's node isn't sided, NetworkAccess tries every face
        LinkerItem.setNetwork(stack, new LinkData.NetworkPos(pos.dimension(), pos.pos(), null));
    }

    @Override
    public void unlink(ItemStack stack) {
        LinkerItem.setNetwork(stack, null);
    }
}
