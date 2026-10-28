package io.ticticboom.mods.mm.port.entity.feature;

import io.ticticboom.mods.mm.port.entity.EntityPortStorage;
import io.ticticboom.mods.mm.port.entity.register.EntityPortBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class EntityCaptureFeature {

    private static final int SCAN_INTERVAL = 10;

    private final EntityPortBlockEntity be;
    private final int scanOffset;

    public EntityCaptureFeature(EntityPortBlockEntity be) {
        this.be = be;
        this.scanOffset = Math.floorMod(be.getBlockPos().hashCode(), SCAN_INTERVAL);
    }

    public void tick() {
        var level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + scanOffset, SCAN_INTERVAL) != 0) {
            return;
        }
        var storage = be.getPortStorage();
        storage.refresh();
        if (be.isInput()) {
            capture(storage);
        }
    }

    private void capture(EntityPortStorage storage) {
        if (storage.isFull()) {
            return;
        }
        var level = be.getLevel();
        var candidates = level.getEntitiesOfClass(LivingEntity.class, storage.zone(),
                candidate -> candidate.isAlive() && !(candidate instanceof Player));
        for (var candidate : candidates) {
            if (storage.isFull()) {
                return;
            }
            storage.absorb(candidate);
        }
    }
}
