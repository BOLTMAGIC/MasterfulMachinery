package io.ticticboom.mods.mm.port.entity.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.entity.EntityPortMode;
import io.ticticboom.mods.mm.port.entity.EntityPortStorageModel;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity = EntityPortStorageModel.DEFAULT_CAPACITY;
    private EntityPortMode mode = EntityPortMode.STORED;
    private boolean invulnerable = false;
    private boolean immobile = true;
    private boolean silent = false;
    private Boolean persistent = null;
    private Boolean consume = null;
    private double speedPerEntity = 0;
    private int zoneWidth = 1;
    private int zoneHeight = 1;
    private int zoneDepth = 1;
    private final List<Identifier> entities = new ArrayList<>();
    private final List<Identifier> tags = new ArrayList<>();

    public EntityConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public EntityConfigBuilderJS mode(String mode) {
        this.mode = EntityPortMode.parse(mode);
        return this;
    }

    public EntityConfigBuilderJS invulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }

    public EntityConfigBuilderJS immobile(boolean immobile) {
        this.immobile = immobile;
        return this;
    }

    public EntityConfigBuilderJS silent(boolean silent) {
        this.silent = silent;
        return this;
    }

    public EntityConfigBuilderJS persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public EntityConfigBuilderJS consume(boolean consume) {
        this.consume = consume;
        return this;
    }

    public EntityConfigBuilderJS speedPerEntity(double speedPerEntity) {
        this.speedPerEntity = Math.max(0, speedPerEntity);
        return this;
    }

    public EntityConfigBuilderJS zone(int size) {
        return zone(size, size, size);
    }

    public EntityConfigBuilderJS zone(int width, int height, int depth) {
        this.zoneWidth = EntityPortStorageModel.clampZone(width);
        this.zoneHeight = EntityPortStorageModel.clampZone(height);
        this.zoneDepth = EntityPortStorageModel.clampZone(depth);
        return this;
    }

    public EntityConfigBuilderJS entity(String entity) {
        this.entities.add(Identifier.parse(entity));
        return this;
    }

    public EntityConfigBuilderJS tag(String tag) {
        this.tags.add(Identifier.parse(tag));
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new EntityPortStorageModel(
                capacity <= 0 ? EntityPortStorageModel.DEFAULT_CAPACITY : capacity,
                mode,
                invulnerable,
                immobile,
                silent,
                persistent,
                consume == null ? mode == EntityPortMode.STORED : consume,
                speedPerEntity,
                zoneWidth,
                zoneHeight,
                zoneDepth,
                Collections.unmodifiableList(new ArrayList<>(entities)),
                Collections.unmodifiableList(new ArrayList<>(tags)),
                getTierRank());
    }
}
