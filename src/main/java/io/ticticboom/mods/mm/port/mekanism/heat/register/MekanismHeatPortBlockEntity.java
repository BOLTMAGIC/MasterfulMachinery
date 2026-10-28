package io.ticticboom.mods.mm.port.mekanism.heat.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorage;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorageModel;
import io.ticticboom.mods.mm.port.mekanism.heat.feature.MekanismHeatPortTransferFeature;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MekanismHeatPortBlockEntity extends AbstractPortBlockEntity {
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    private final MekanismHeatPortStorage storage;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<MekanismHeatPortTransferFeature> transfer;

    public MekanismHeatPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        this.storage = (MekanismHeatPortStorage) model.config().createPortStorage(this::setChanged);
        var enabled = ((MekanismHeatPortStorageModel) storage.getStorageModel()).autoPush().get();
        this.transfer = enabled ? Optional.of(new MekanismHeatPortTransferFeature(this)) : Optional.empty();
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public MekanismHeatPortStorageModel getStorageModel() {
        return (MekanismHeatPortStorageModel) storage.getStorageModel();
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
        return Component.translatable("port.mm.mekanism_heat.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new MekanismHeatPortMenu(model, groupHolder, isInput, windowId, inv, this);
    }

    @Override
    protected void saveLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, storage.save(new CompoundTag(), registries));
        super.saveLegacy(tag, registries);
    }

    @Override
    protected void loadLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        storage.load(tag.getCompound(Ref.NBT_STORAGE_KEY), registries);
        super.loadLegacy(tag, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveLegacy(tag, registries);
        return tag;
    }

    @Override
    public void setChanged() {
        assert level != null;
        if (level.isClientSide()) {
            return;
        }
        super.setChanged();
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        transfer.ifPresent(MekanismHeatPortTransferFeature::tick);
    }

    @Override
    public void onLoad() {
        transfer.ifPresent(MekanismHeatPortTransferFeature::onLoad);
    }

    public void neighborsChanged() {
        transfer.ifPresent(MekanismHeatPortTransferFeature::neighborsChanged);
    }
}
