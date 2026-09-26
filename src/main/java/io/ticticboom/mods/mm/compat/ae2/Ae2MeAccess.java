package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import io.ticticboom.mods.mm.builder.me.CraftHandle;
import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The AE2 network a Multiblock Tool is bound to. Reached like the network linker's (loaded chunk, active node, no
 * force-loading); every extraction runs as the player, so AE2's own security applies.
 */
public final class Ae2MeAccess implements MeAccess {
    private final ServerPlayer player;
    private final LinkData.NetworkPos network;
    @Nullable
    private IGrid grid;
    /** Stock copied from the grid at the first {@link #stock} call after a refresh; lowered by own extractions. */
    @Nullable
    private KeyCounter stock;

    private Ae2MeAccess(ServerPlayer player, LinkData.NetworkPos network) {
        this.player = player;
        this.network = network;
    }

    /** The {@code MeAccessFactory} lookup: null when the tool isn't bound, ME is turned off, or the network is unreachable. */
    @Nullable
    public static MeAccess forTool(ServerPlayer player, ItemStack tool) {
        LinkData.NetworkPos network = ToolData.network(tool);
        if (network == null || !ToolData.useMe(tool)) {
            return null;
        }
        Ae2MeAccess access = new Ae2MeAccess(player, network);
        access.refresh();
        return access.reachable() ? access : null;
    }

    @Override
    public void refresh() {
        grid = NetworkAccess.grid(player.server, network);
        stock = null;
    }

    @Override
    public boolean reachable() {
        return grid != null;
    }

    @Override
    public long stock(Item item) {
        if (grid == null) {
            return 0;
        }
        if (stock == null) {
            stock = new KeyCounter();
            stock.addAll(grid.getStorageService().getCachedInventory());
        }
        return stock.get(AEItemKey.of(item));
    }

    @Override
    public ItemStack extract(Item item, int amount, boolean simulate) {
        if (grid == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = AEItemKey.of(item);
        long got = StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(), key, amount,
                IActionSource.ofPlayer(player), Actionable.ofSimulate(simulate));
        if (got <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate && stock != null) {
            stock.remove(key, got);
        }
        return key.toStack((int) got);
    }

    @Override
    public boolean isCraftable(Item item) {
        return grid != null && grid.getCraftingService().isCraftable(AEItemKey.of(item));
    }

    @Override
    public CraftHandle requestCraft(Item item, int amount) {
        // crafting requests arrive with the craft tracker
        throw new UnsupportedOperationException("ME crafting requests are not supported yet");
    }
}