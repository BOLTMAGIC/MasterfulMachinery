package io.ticticboom.mods.mm.port.replication.link.register;

import com.buuz135.replication.api.pattern.IMatterPatternHolder;
import com.buuz135.replication.api.pattern.MatterPattern;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.replication.link.ReplicationLinkPortStorage;
import io.ticticboom.mods.mm.port.replication.link.feature.ReplicationLinkFeature;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReplicationLinkPortBlockEntity extends AbstractPortBlockEntity implements IMatterPatternHolder<ReplicationLinkPortBlockEntity> {

    private final PortModel model;
    private final ReplicationLinkPortStorage storage;
    private final ReplicationLinkFeature feature;

    public ReplicationLinkPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.storage = (ReplicationLinkPortStorage) model.config().createPortStorage(this::setChanged);
        this.feature = new ReplicationLinkFeature(this);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public ReplicationLinkPortStorage getLinkStorage() {
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
        return Component.translatable("port.mm.replication_link.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return null;
    }

    @Override
    public int getPatternSlots(ReplicationLinkPortBlockEntity holder) {
        return feature.getPatterns().size();
    }

    @Override
    public List<MatterPattern> getPatterns(Level level, ReplicationLinkPortBlockEntity holder) {
        return feature.getPatterns();
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
        feature.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        feature.onRemoved();
        super.setRemoved();
    }
}
