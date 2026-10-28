package io.ticticboom.mods.mm.port.replication.matter.register;

import com.buuz135.replication.api.matter_fluid.IMatterTank;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorageModel;
import io.ticticboom.mods.mm.port.replication.matter.feature.ReplicationMatterNetworkFeature;
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

import java.util.List;
import java.util.Optional;

public abstract class ReplicationMatterPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    private final ReplicationMatterPortStorage storage;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ReplicationMatterNetworkFeature> network;

    public ReplicationMatterPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        this.storage = (ReplicationMatterPortStorage) model.config().createPortStorage(this::setChanged);
        var enabled = ((ReplicationMatterPortStorageModel) storage.getStorageModel()).network().get();
        this.network = enabled ? Optional.of(new ReplicationMatterNetworkFeature(this)) : Optional.empty();
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public ReplicationMatterPortStorageModel getStorageModel() {
        return (ReplicationMatterPortStorageModel) storage.getStorageModel();
    }

    public List<? extends IMatterTank> getTanks() {
        return storage.getHandler().tanks();
    }

    public int getPriority() {
        return storage.getPriority();
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
        return Component.translatable("port.mm.replication_matter.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ReplicationMatterPortMenu(model, groupHolder, isInput, windowId, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, storage.save(new CompoundTag(), registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        storage.load(tag.getCompoundOrEmpty(Ref.NBT_STORAGE_KEY), registries);
        super.loadAdditional(tag, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void setChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        super.setChanged();
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        network.ifPresent(ReplicationMatterNetworkFeature::tick);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        network.ifPresent(ReplicationMatterNetworkFeature::onLoad);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        network.ifPresent(ReplicationMatterNetworkFeature::onChunkUnloaded);
    }

    @Override
    public void setRemoved() {
        network.ifPresent(ReplicationMatterNetworkFeature::onRemoved);
        super.setRemoved();
    }
}
