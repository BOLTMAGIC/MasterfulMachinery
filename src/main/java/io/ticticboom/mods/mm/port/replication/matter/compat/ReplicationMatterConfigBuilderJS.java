package io.ticticboom.mods.mm.port.replication.matter.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorageModel;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplicationMatterConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity = ReplicationMatterPortStorageModel.DEFAULT_CAPACITY;
    private final List<Identifier> matter = new ArrayList<>();
    private int tanks = 0;
    private int priority = 0;
    private boolean network = true;

    public ReplicationMatterConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public ReplicationMatterConfigBuilderJS matter(String type) {
        this.matter.add(Identifier.parse(type));
        return this;
    }

    public ReplicationMatterConfigBuilderJS tanks(int tanks) {
        this.tanks = tanks;
        return this;
    }

    public ReplicationMatterConfigBuilderJS priority(int priority) {
        this.priority = priority;
        return this;
    }

    public ReplicationMatterConfigBuilderJS network(boolean network) {
        this.network = network;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        var types = new ArrayList<>(matter);
        while (types.size() < Math.max(1, Math.max(tanks, matter.size()))) {
            types.add(null);
        }
        return new ReplicationMatterPortStorageModel(
                capacity <= 0 ? ReplicationMatterPortStorageModel.DEFAULT_CAPACITY : capacity,
                Collections.unmodifiableList(types),
                () -> network,
                ReplicationMatterPortStorageModel.clampPriority(priority),
                getTierRank());
    }
}
