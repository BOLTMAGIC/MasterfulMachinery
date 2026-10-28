package io.ticticboom.mods.mm.port.energy.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorageModel;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortTransfers;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class  EnergyPortBlockEntity extends AbstractPortBlockEntity {
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    private final EnergyPortStorage storage;

    public EnergyPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        storage = (EnergyPortStorage) model.config().createPortStorage(this::setChanged);
        var enabledByDefault = !isInput && ((EnergyPortStorageModel) storage.getStorageModel()).autoPush().get();
        autoIO = new PortAutoIO(this, isInput, enabledByDefault, PortTransfers.energy(storage::getHandler));
    }
    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public EnergyPortStorageModel getStorageModel() {
        return (EnergyPortStorageModel) storage.getStorageModel();
    }

    @Override
    public boolean isInput() {
        return isInput;
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("port.mm.energy.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new EnergyPortMenu(model, groupHolder, isInput, windowId, inv, this);
    }
}
