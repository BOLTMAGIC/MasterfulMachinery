package io.ticticboom.mods.mm.port.nuclear.radiation.register;

import net.minecraft.world.Containers;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortHazard;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortStorage;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortStorageModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NuclearRadiationPortBlockEntity extends AbstractPortBlockEntity {

    private static final int HAZARD_REFRESH = 100;
    private static final int HAZARD_MIN_INTERVAL = 20;

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;
    private final NuclearRadiationPortStorage storage;
    private final NuclearRadiationPortHazard hazard;
    private boolean hazardDirty = true;
    private long lastHazardUpdate = Long.MIN_VALUE / 2;

    public NuclearRadiationPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        this.storage = (NuclearRadiationPortStorage) model.config().createPortStorage(this::contentsChanged);
        this.storage.setInputSide(isInput);
        var storageModel = (NuclearRadiationPortStorageModel) storage.getStorageModel();
        this.hazard = storageModel.shielded() ? null : new NuclearRadiationPortHazard();
    }

    private void contentsChanged() {
        hazardDirty = true;
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide() || lastTick == level.getGameTime()) {
            return;
        }
        lastTick = level.getGameTime();
        storage.tick(lastTick);
        long sinceUpdate = lastTick - lastHazardUpdate;
        if (hazard != null && level instanceof ServerLevel server
                && ((hazardDirty && sinceUpdate >= HAZARD_MIN_INTERVAL) || sinceUpdate >= HAZARD_REFRESH)) {
            hazard.update(server, getBlockPos(), storage.profile(lastTick));
            hazardDirty = false;
            lastHazardUpdate = lastTick;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (hazard != null && level instanceof ServerLevel server) {
            hazard.remove(server);
        }
    }

    public NuclearRadiationPortStorage getRadiationStorage() {
        return storage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("port.mm.nuclear_radiation.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new NuclearRadiationPortMenu(model, groupHolder, windowId, this, inventory);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public boolean isInput() {
        return isInput;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            var items = getRadiationStorage().getItems();
            for (int i = 0; i < items.getSlots(); i++) {
                var stack = items.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
}
