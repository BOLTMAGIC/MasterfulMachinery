package io.ticticboom.mods.mm.port.projecte.emc.register;

import net.minecraft.world.Containers;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortStorage;
import io.ticticboom.mods.mm.port.projecte.emc.feature.ProjectEEmcPushFeature;
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

public class ProjectEEmcPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final ProjectEEmcPortStorage storage;
    private final ProjectEEmcPushFeature feature;

    public ProjectEEmcPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (ProjectEEmcPortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.setInput(model.input());
        this.feature = new ProjectEEmcPushFeature(this);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public ProjectEEmcPortStorage getEmcStorage() {
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
        return Component.translatable("port.mm.projecte_emc.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ProjectEEmcPortMenu(model, groupHolder, windowId, inv, this);
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        storage.tickKlein();
        feature.tick();
        updateFillState();
    }

    private void updateFillState() {
        var state = getBlockState();
        if (!state.hasProperty(ProjectEEmcPortBlock.FILL)) {
            return;
        }
        int stage = fillStage();
        if (state.getValue(ProjectEEmcPortBlock.FILL) != stage) {
            level.setBlock(getBlockPos(), state.setValue(ProjectEEmcPortBlock.FILL, stage), Block.UPDATE_CLIENTS);
        }
    }

    private int fillStage() {
        if (storage.getStored() <= 0 || storage.getCapacity() <= 0) {
            return 0;
        }
        double ratio = (double) storage.getStored() / storage.getCapacity();
        return Math.max(1, Math.min(ProjectEEmcPortBlock.MAX_FILL, (int) Math.ceil(ratio * ProjectEEmcPortBlock.MAX_FILL)));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        feature.onLoad();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            var stack = getEmcStorage().getKlein().getStackInSlot(0);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
}
