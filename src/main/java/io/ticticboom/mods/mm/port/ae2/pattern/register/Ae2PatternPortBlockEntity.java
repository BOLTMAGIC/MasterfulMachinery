package io.ticticboom.mods.mm.port.ae2.pattern.register;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.ae2.pattern.Ae2PatternPortStorage;
import io.ticticboom.mods.mm.port.ae2.pattern.feature.Ae2PatternFeature;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Ae2PatternPortBlockEntity extends AbstractPortBlockEntity implements IInWorldGridNodeHost, IActionHost {

    private final PortModel model;
    private final Ae2PatternPortStorage storage;
    private final Ae2PatternFeature feature;

    public Ae2PatternPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.storage = (Ae2PatternPortStorage) model.config().createPortStorage(this::setChanged);
        this.feature = new Ae2PatternFeature(this);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public Ae2PatternPortStorage getPatternStorage() {
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
        return Component.translatable("port.mm.ae2_pattern.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return null;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return feature.getNode().getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return feature.getNode().getNode();
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        feature.tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        feature.onLoad();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        feature.onRemoved();
    }

    @Override
    public void setRemoved() {
        feature.onRemoved();
        super.setRemoved();
    }

    @Override
    protected void saveLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLegacy(tag, registries);
        feature.saveNode(tag);
    }

    @Override
    protected void loadLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLegacy(tag, registries);
        feature.loadNode(tag);
    }
}
