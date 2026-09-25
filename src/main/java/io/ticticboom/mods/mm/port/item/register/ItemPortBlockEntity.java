package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.model.PortModel;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    @Getter
    private final boolean input;

    public ItemPortBlockEntity(RegistryGroupHolder groupHolder, PortModel model, boolean input, BlockPos pos,
                               BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.groupHolder = groupHolder;
        this.model = model;
        storage = (ItemPortStorage) model.config().createPortStorage(this::notifyMenuChanged);
        this.input = input;
        var enabledByDefault = !input && ((ItemPortStorageModel) storage.getStorageModel()).autoPush().get();
        autoIO = new PortAutoIO(this, input, enabledByDefault, PortTransfers.items(storage::getHandler));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return storage.getCapability(cap);
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

    /**
     * Called by child storages/handlers when their contents change.
     * Notifies the chunk (setChanged) and also broadcasts container changes to any player
     * that has this block entity's menu open so their GUI updates immediately.
     */
    public void notifyMenuChanged() {
        // mark dirty and send block update
        setChanged();
        if (level == null || level.isClientSide) return;
        if (level instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                try {
                    AbstractContainerMenu menu = sp.containerMenu;
                    if (menu instanceof io.ticticboom.mods.mm.port.item.register.ItemPortMenu ipm) {
                        // compare block entity
                        Object be = ipm.getBlockEntity();
                        if (be == this) {
                            ipm.broadcastChanges();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
