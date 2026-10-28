package io.ticticboom.mods.mm.port.entity.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.entity.EntityPortStorage;
import io.ticticboom.mods.mm.port.entity.EntityPortStorageModel;
import io.ticticboom.mods.mm.port.entity.feature.EntityCaptureFeature;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;
    private final EntityPortStorage storage;
    private final EntityCaptureFeature capture;

    public EntityPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        this.storage = (EntityPortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.setOwner(this);
        this.capture = new EntityCaptureFeature(this);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public EntityPortStorage getPortStorage() {
        return storage;
    }

    public EntityPortStorageModel getStorageModel() {
        return (EntityPortStorageModel) storage.getStorageModel();
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
        return Component.translatable("port.mm.entity.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new EntityPortMenu(model, groupHolder, isInput, windowId, inv, this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put(Ref.NBT_STORAGE_KEY, storage.saveForClient(new CompoundTag()));
        return tag;
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) {
            return;
        }
        lastTick = level.getGameTime();
        capture.tick();
    }

    public void releaseHeldEntities() {
        storage.releaseAll();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        releaseHeldEntities();
        super.preRemoveSideEffects(pos, state);
    }
}
