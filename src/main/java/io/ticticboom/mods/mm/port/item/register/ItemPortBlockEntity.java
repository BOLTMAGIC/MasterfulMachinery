package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.world.Containers;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

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
        storage = (ItemPortStorage) model.config().createPortStorage(this::setChanged);
        this.input = input;
        var enabledByDefault = !input && ((ItemPortStorageModel) storage.getStorageModel()).autoPush().get();
        autoIO = new PortAutoIO(this, input, enabledByDefault, PortTransfers.items(storage::getHandler));
    }
    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("port.mm.item.title");
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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            NetworkLink.beforePortRemoved(level, pos);
            Containers.dropContents(level, pos, ((ItemPortStorage) getStorage()).getHandler().getStacks());
        }
        super.preRemoveSideEffects(pos, state);
    }
}
