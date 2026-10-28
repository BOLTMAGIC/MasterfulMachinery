package io.ticticboom.mods.mm.compat.ae2;

import io.ticticboom.mods.mm.compat.ae2.linker.NetworkAccess;
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

public final class Ae2MeAccess implements MeAccess {
    private final ServerPlayer player;
    private final LinkData.NetworkPos network;
    @Nullable
    private IGrid grid;
    private final Map<AEItemKey, Long> extracted = new HashMap<>();

    private Ae2MeAccess(ServerPlayer player, LinkData.NetworkPos network) {
        this.player = player;
        this.network = network;
    }

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

    @Nullable
    public static Component problem(ServerPlayer player, ItemStack tool) {
        LinkData.NetworkPos network = ToolData.network(tool);
        if (network == null || !ToolData.useMe(tool)) {
            return null;
        }
        IGrid grid = NetworkAccess.grid(player.level().getServer(), network);
        String key;
        if (grid == null) {
            key = "message.mm.tool.me_unreachable";
        } else if (!NetworkAccess.mayUse(player, grid)) {
            key = "message.mm.tool.me_denied";
        } else {
            return null;
        }
        return Component.translatable(key, network.pos().toShortString(), network.dimension().identifier().getPath())
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void refresh() {
        grid = usableGrid();
        extracted.clear();
    }

    @Nullable
    private IGrid usableGrid() {
        IGrid found = NetworkAccess.grid(player.level().getServer(), network);
        return found != null && NetworkAccess.mayUse(player, found) ? found : null;
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
                IActionSource.ofPlayer(player), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
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
    public long requestedAmount(Item item) {
        return grid == null ? 0 : grid.getCraftingService().getRequestedAmount(AEItemKey.of(item));
    }

    @Override
    public CraftHandle requestCraft(Item item, int amount) {
        if (grid == null) {
            return CraftHandle.failed(item, amount, Component.translatable("message.mm.tool.craft.unreachable"));
        }
        return Ae2CraftHandle.start(player, grid, this::usableGrid, item, amount);
    }
}
