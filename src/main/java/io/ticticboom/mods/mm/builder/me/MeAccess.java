package io.ticticboom.mods.mm.builder.me;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Common code's view of the AE2 network a Multiblock Tool is bound to. No {@code appeng.*} types
 * appear here; the real implementation is {@code compat.ae2.Ae2MeAccess}, produced through
 * {@link MeAccessFactory}.
 */
public interface MeAccess {
    /** Whether the bound network can currently be reached (its chunk loaded, the node still there). */
    boolean reachable();

    /** Snapshot stock of {@code item} in the network, for planning; not a live value. */
    long stock(Item item);

    /**
     * Extracts up to {@code amount} of {@code item} from the network.
     *
     * @param simulate when true, nothing is actually removed
     * @return what was (or would be) extracted; may hold fewer than {@code amount}
     */
    ItemStack extract(Item item, int amount, boolean simulate);

    /** Whether the network has a pattern able to craft {@code item}. */
    boolean isCraftable(Item item);

    /** Requests a craft of {@code amount} of {@code item}; the returned handle tracks its progress. */
    CraftHandle requestCraft(Item item, int amount);
}
