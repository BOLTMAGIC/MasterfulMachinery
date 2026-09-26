package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import io.ticticboom.mods.mm.builder.me.CraftHandle;
import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The AE2 network a Multiblock Tool is bound to. Reached like the network linker's (loaded chunk, active node, no
 * force-loading) and only by players the network's owners allow ({@link NetworkAccess#mayUse}); AE2 15 has no security
 * service, so that check is the only one. Extractions run as the player.
 */
public final class Ae2MeAccess implements MeAccess {
    private final ServerPlayer player;
    private final LinkData.NetworkPos network;
    @Nullable
    private IGrid grid;
    /** What this access extracted since the last refresh; AE2's cached stock only catches up at the end of a tick. */
    private final Map<AEItemKey, Long> extracted = new HashMap<>();

    private Ae2MeAccess(ServerPlayer player, LinkData.NetworkPos network) {
        this.player = player;
        this.network = network;
    }

    /** The {@code MeAccessFactory} lookup: null when the tool isn't bound, ME is turned off, or the network can't be used. */
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

    /** Why {@link #forTool} gives null for a bound tool with ME turned on (unreachable, or no access); else null. */
    @Nullable
    public static Component problem(ServerPlayer player, ItemStack tool) {
        LinkData.NetworkPos network = ToolData.network(tool);
        if (network == null || !ToolData.useMe(tool)) {
            return null;
        }
        IGrid grid = NetworkAccess.grid(player.server, network);
        String key;
        if (grid == null) {
            key = "message.mm.tool.me_unreachable";
        } else if (!NetworkAccess.mayUse(player, grid)) {
            key = "message.mm.tool.me_denied";
        } else {
            return null;
        }
        return Component.translatable(key, network.pos().toShortString(), network.dimension().location().getPath())
                .withStyle(ChatFormatting.YELLOW);
    }

    /** Looks the grid up again (it may be gone, or its owners changed) and starts a new tick's stock. */
    @Override
    public void refresh() {
        IGrid found = NetworkAccess.grid(player.server, network);
        grid = found != null && NetworkAccess.mayUse(player, found) ? found : null;
        extracted.clear();
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
        AEItemKey key = AEItemKey.of(item);
        long cached = grid.getStorageService().getCachedInventory().get(key);
        return Math.max(0, cached - extracted.getOrDefault(key, 0L));
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
        if (!simulate) {
            extracted.merge(key, got, Long::sum);
        }
        return key.toStack((int) got);
    }

    @Override
    public boolean isCraftable(Item item) {
        return grid != null && grid.getCraftingService().isCraftable(AEItemKey.of(item));
    }

    @Override
    public CraftHandle requestCraft(Item item, int amount) {
        if (grid == null) {
            return CraftHandle.failed(item, amount, Component.translatable("message.mm.tool.craft.unreachable"));
        }
        return Ae2CraftHandle.start(player, grid, item, amount);
    }
}
