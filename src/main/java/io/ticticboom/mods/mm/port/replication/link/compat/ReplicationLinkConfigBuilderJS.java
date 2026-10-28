package io.ticticboom.mods.mm.port.replication.link.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.replication.link.ReplicationLinkPortStorageModel;

public class ReplicationLinkConfigBuilderJS extends PortConfigBuilderJS {
    @Override
    public IPortStorageModel build() {
        return new ReplicationLinkPortStorageModel();
    }
}
