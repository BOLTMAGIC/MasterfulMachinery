package io.ticticboom.mods.mm.port.ars.source.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortStorage;
import io.ticticboom.mods.mm.port.ars.source.feature.ArsSourceFeature;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArsSourcePortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final ArsSourcePortStorage storage;
    private final ArsSourceFeature feature;

    public ArsSourcePortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (ArsSourcePortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.setInput(model.input());
        this.feature = new ArsSourceFeature(this);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public ArsSourcePortStorage getSourceStorage() {
        return storage;
    }

    @Override
    public boolean isInput() {
        return model.input();
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("port.mm.ars_source.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ArsSourcePortMenu(model, groupHolder, windowId, inv, this);
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        feature.tick();
        updateFillState();
    }

    private void updateFillState() {
        var state = getBlockState();
        if (!state.hasProperty(ArsSourcePortBlock.FILL)) {
            return;
        }
        int stage = fillStage();
        if (state.getValue(ArsSourcePortBlock.FILL) != stage) {
            level.setBlock(getBlockPos(), state.setValue(ArsSourcePortBlock.FILL, stage), Block.UPDATE_CLIENTS);
        }
    }

    private int fillStage() {
        if (storage.getStored() <= 0 || storage.getCapacity() <= 0) {
            return 0;
        }
        double ratio = (double) storage.getStored() / storage.getCapacity();
        return Math.max(1, Math.min(ArsSourcePortBlock.MAX_FILL, (int) Math.ceil(ratio * ArsSourcePortBlock.MAX_FILL)));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        feature.onLoad();
    }

    @Override
    public void setRemoved() {
        feature.onRemoved();
        super.setRemoved();
    }
}
