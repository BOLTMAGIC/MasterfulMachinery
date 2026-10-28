package io.ticticboom.mods.mm.port.ars.source.feature;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortStorage;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class ArsSourceFeature {

    private static final int TRANSFER_INTERVAL = 20;

    private final ArsSourcePortBlockEntity port;
    private final PortProvider provider = new PortProvider();
    private boolean detached = false;

    public ArsSourceFeature(ArsSourcePortBlockEntity port) {
        this.port = port;
    }

    public void onLoad() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        detached = false;
        SourceManager.INSTANCE.addInterface(level, provider);
    }

    public void onRemoved() {
        detached = true;
    }

    public void tick() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % TRANSFER_INTERVAL != 0) {
            return;
        }
        if (port.isInput()) {
            pull(level);
        } else {
            push(level);
        }
    }

    private void pull(ServerLevel level) {
        var storage = port.getSourceStorage();
        int room = storage.getCapacity() - storage.getStored();
        if (room <= 0) {
            return;
        }
        for (ISpecialSourceProvider nearby : SourceUtil.canTakeSource(port.getBlockPos(), level, storage.getRange())) {
            if (room <= 0) {
                return;
            }
            if (!usable(nearby)) {
                continue;
            }
            var tile = nearby.getSource();
            int taken = Math.min(tile.getSource(), room);
            if (taken <= 0) {
                continue;
            }
            tile.removeSource(taken);
            room -= storage.receive(taken, false);
        }
    }

    private void push(ServerLevel level) {
        var storage = port.getSourceStorage();
        for (ISpecialSourceProvider nearby : SourceUtil.canGiveSource(port.getBlockPos(), level, storage.getRange())) {
            if (storage.getStored() <= 0) {
                return;
            }
            if (!usable(nearby)) {
                continue;
            }
            var tile = nearby.getSource();
            int given = Math.min(storage.getStored(), tile.getMaxSource() - tile.getSource());
            if (given <= 0) {
                continue;
            }
            tile.addSource(given);
            storage.extract(given, false);
        }
    }

    private boolean usable(ISpecialSourceProvider nearby) {
        return nearby.isValid() && !nearby.getCurrentPos().equals(port.getBlockPos());
    }

    private class PortProvider implements ISpecialSourceProvider {

        private final PortSourceTile tile = new PortSourceTile();

        @Override
        public ISourceTile getSource() {
            return tile;
        }

        @Override
        public boolean isValid() {
            return !detached && !port.isRemoved();
        }

        @Override
        public BlockPos getCurrentPos() {
            return port.getBlockPos();
        }
    }

    private class PortSourceTile implements ISourceTile {

        private ArsSourcePortStorage storage() {
            return port.getSourceStorage();
        }

        @Override
        public int getTransferRate() {
            return storage().getCapacity();
        }

        @Override
        public boolean canAcceptSource() {
            return port.isInput() && storage().getStored() < storage().getCapacity();
        }

        @Override
        public int getSource() {
            return port.isInput() ? 0 : storage().getStored();
        }

        @Override
        public int getMaxSource() {
            return storage().getCapacity();
        }

        @Override
        public boolean canProvideSource() {
            return !port.isInput() && storage().getStored() > 0;
        }

        @Override
        public int setSource(int source) {
            int target = Math.max(0, Math.min(storage().getCapacity(), source));
            int stored = storage().getStored();
            if (target > stored) {
                storage().receive(target - stored, false);
            } else if (target < stored && !port.isInput()) {
                storage().extract(stored - target, false);
            }
            return storage().getStored();
        }

        @Override
        public int addSource(int source) {
            if (port.isInput()) {
                storage().receive(source, false);
            }
            return storage().getStored();
        }

        @Override
        public int removeSource(int source) {
            if (!port.isInput()) {
                storage().extract(source, false);
            }
            return storage().getStored();
        }
    }
}
