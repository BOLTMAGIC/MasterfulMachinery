package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.compat.ae2.Ae2ItemPortStorage;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortTransfers;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemPortBlockEntity extends AbstractPortBlockEntity {
    private final RegistryGroupHolder groupHolder;
    private final PortModel model;

    private final ItemPortStorage storage;
    private final LazyOptional<?> meStorage;

    @Getter
    private final boolean input;

    // while nobody has the port's screen open, contents go to clients at most this often (ticks)
    private static final int UNWATCHED_SYNC_INTERVAL = 20;

    private boolean contentsSyncPending;
    private long lastContentsSync;
    // players with this port's screen open (server side)
    private int viewers;

    public ItemPortBlockEntity(RegistryGroupHolder groupHolder, PortModel model, boolean input, BlockPos pos,
                               BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.groupHolder = groupHolder;
        this.model = model;
        storage = (ItemPortStorage) model.config().createPortStorage(this::notifyMenuChanged);
        meStorage = NetworkLink.AVAILABLE
                ? LazyOptional.of(() -> new Ae2ItemPortStorage(storage, Component.translatable("container.mm.port.item")))
                : LazyOptional.empty();
        this.input = input;
        var enabledByDefault = !input && ((ItemPortStorageModel) storage.getStorageModel()).autoPush().get();
        autoIO = new PortAutoIO(this, input, enabledByDefault, PortTransfers.items(storage::getHandler));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (NetworkLink.AVAILABLE && Ae2ItemPortStorage.isStorageCapability(cap)) {
            return meStorage.cast();
        }
        return storage.getCapability(cap);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        meStorage.invalidate();
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mm.port.item");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ItemPortMenu(model, groupHolder, input, i, inventory, this);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public void tick() {
        super.tick();
        if (!contentsSyncPending || level == null || level.isClientSide) {
            return;
        }

        // a full update re-serializes every slot; a busy machine changes its ports every tick
        if (viewers == 0 && level.getGameTime() - lastContentsSync < UNWATCHED_SYNC_INTERVAL) {
            return;
        }
        contentsSyncPending = false;
        lastContentsSync = level.getGameTime();
        syncBlockEntityToClients();
    }

    /** A player opened this port's screen: its slots show the synced contents, so send them every tick again. */
    public void addViewer() {
        viewers++;
        if (contentsSyncPending && level != null && !level.isClientSide) {
            contentsSyncPending = false;
            lastContentsSync = level.getGameTime();
            syncBlockEntityToClients();
        }
    }

    public void removeViewer() {
        viewers = Math.max(0, viewers - 1);
    }

    /**
     * Called by child storages/handlers when their contents change.
     * Marks the chunk dirty and coalesces client sync to one full block-entity update per tick while the port's
     * screen is open, one per {@link #UNWATCHED_SYNC_INTERVAL} ticks otherwise.
     */
    public void notifyMenuChanged() {
        if (level == null || level.isClientSide) return;
        markChangedWithoutSync();
        contentsSyncPending = true;
    }
}
