package io.ticticboom.mods.mm.port.replication.matter.register;

import com.buuz135.replication.api.network.IMatterTanksConsumer;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ReplicationMatterInputPortBlockEntity extends ReplicationMatterPortBlockEntity implements IMatterTanksConsumer {

    public ReplicationMatterInputPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(model, groupHolder, true, pos, state);
    }
}
